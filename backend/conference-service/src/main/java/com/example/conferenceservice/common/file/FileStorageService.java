package com.example.conferenceservice.common.file;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// EC2 단일 서버 + docker compose 배포 구조라 로컬 디스크에 저장한다.
// infra/compose.yaml에서 이 경로를 호스트 볼륨으로 마운트해야 재배포 시에도 파일이 유지된다.
@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_PROOF_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");
    // 소개글 본문 이미지 파일명 접두사 - 대표 이미지(thumb_/detail_)와 구분하기 위함.
    private static final String DESCRIPTION_IMAGE_PREFIX = "desc_";

    // 목록 카드용 썸네일 - 트래픽 절약이 목적이라 원본보다 훨씬 작게 잡는다.
    private static final int THUMBNAIL_MAX_DIMENSION = 400;
    // 상세 페이지용 이미지 - 화질은 유지하되 지나치게 큰 원본 업로드를 방지하는 상한선.
    private static final int DETAIL_MAX_DIMENSION = 1600;
    // 압축률이 높은 파일(용량은 작지만 픽셀 수는 방대한 이미지)로 디코딩 시 메모리를 고갈시키는
    // 압축폭탄을 막기 위해, 실제 픽셀 버퍼를 할당(read)하기 전에 헤더만으로 가로*세로를 검사한다.
    private static final long MAX_PIXELS = 30_000_000L; // 약 30MP (예: 6000x5000)
    private static final Map<String, String> CANONICAL_EXTENSION_BY_FORMAT = Map.of(
            "png", "png",
            "jpeg", "jpg"
    );

    private final Path proofRootDir;
    private final Path imageRootDir;

    public FileStorageService(
            @Value("${app.upload.conference-proof-dir}") String proofUploadDir,
            @Value("${app.upload.conference-image-dir}") String imageUploadDir
    ) {
        this.proofRootDir = createRootDir(proofUploadDir);
        this.imageRootDir = createRootDir(imageUploadDir);
    }

    private Path createRootDir(String uploadDir) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + root, e);
        }
        return root;
    }

    public String store(MultipartFile file) {
        String originalFilename = cleanOriginalFilename(file);
        validateExtension(originalFilename, ALLOWED_PROOF_EXTENSIONS, ConferenceErrorCode.PROOF_FILE_INVALID_TYPE);

        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        Path target = resolveWithinRoot(proofRootDir, storedFilename, ConferenceErrorCode.PROOF_FILE_UPLOAD_FAILED);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return storedFilename;
        } catch (IOException e) {
            log.error("증명 파일 저장 실패: {}", originalFilename, e);
            throw new BusinessException(ConferenceErrorCode.PROOF_FILE_UPLOAD_FAILED);
        }
    }

    // 목록용 썸네일과 상세용 이미지를 각각 리사이징해서 저장한다.
    // 원본을 그대로 두 번 내려주면 목록 페이지에서 카드 수십 개가 동시에 큰 원본을 받아와 대역폭을 낭비하기 때문.
    public StoredImage storeImage(MultipartFile file) {
        String originalFilename = cleanOriginalFilename(file);
        validateExtension(originalFilename, ALLOWED_IMAGE_EXTENSIONS, ConferenceErrorCode.IMAGE_INVALID_TYPE);

        Path thumbnailTarget = null;
        Path detailTarget = null;
        try {
            byte[] bytes = file.getBytes();
            ImageHeader header = readImageHeader(bytes);
            // 저장 파일명은 사용자가 올린 원본 파일명이 아니라, 실제로 디코딩된 포맷을 근거로 짓는다 -
            // 그래야 "poster.png"라는 이름의 JPEG처럼 확장자와 실제 포맷이 어긋나는 경우가 없다.
            String baseName = UUID.randomUUID() + "." + header.extension();
            String thumbnailFilename = "thumb_" + baseName;
            String detailFilename = "detail_" + baseName;
            thumbnailTarget = resolveWithinRoot(imageRootDir, thumbnailFilename, ConferenceErrorCode.IMAGE_UPLOAD_FAILED);
            detailTarget = resolveWithinRoot(imageRootDir, detailFilename, ConferenceErrorCode.IMAGE_UPLOAD_FAILED);

            // 두 상한 중 더 작은 썸네일 상한 이내면 상세용도 당연히 리사이징이 필요 없다는 뜻이라,
            // 굳이 전체 픽셀을 디코딩하지 않고 원본 바이트를 그대로 양쪽에 저장한다.
            if (header.width() <= THUMBNAIL_MAX_DIMENSION && header.height() <= THUMBNAIL_MAX_DIMENSION) {
                Files.write(thumbnailTarget, bytes);
                Files.write(detailTarget, bytes);
            } else {
                BufferedImage decoded = decode(bytes);
                writeCapped(decoded, bytes, THUMBNAIL_MAX_DIMENSION, 0.85, thumbnailTarget);
                writeCapped(decoded, bytes, DETAIL_MAX_DIMENSION, 0.9, detailTarget);
            }
            return new StoredImage(thumbnailFilename, detailFilename);
        } catch (IOException e) {
            log.error("이미지 저장 실패: {}", originalFilename, e);
            deleteQuietly(thumbnailTarget);
            deleteQuietly(detailTarget);
            throw new BusinessException(ConferenceErrorCode.IMAGE_UPLOAD_FAILED);
        } catch (RuntimeException e) {
            deleteQuietly(thumbnailTarget);
            deleteQuietly(detailTarget);
            throw e;
        }
    }

    // 소개글 본문에 삽입하는 이미지 - 대표 이미지(썸네일+상세 두 장)와 달리 본문 안에 한 장만
    // 필요해서 상세용 상한(DETAIL_MAX_DIMENSION) 하나만 적용해 저장한다.
    public String storeDescriptionImage(MultipartFile file) {
        String originalFilename = cleanOriginalFilename(file);
        validateExtension(originalFilename, ALLOWED_IMAGE_EXTENSIONS, ConferenceErrorCode.IMAGE_INVALID_TYPE);

        Path target = null;
        try {
            byte[] bytes = file.getBytes();
            ImageHeader header = readImageHeader(bytes);
            String filename = DESCRIPTION_IMAGE_PREFIX + UUID.randomUUID() + "." + header.extension();
            target = resolveWithinRoot(imageRootDir, filename, ConferenceErrorCode.IMAGE_UPLOAD_FAILED);

            if (header.width() <= DETAIL_MAX_DIMENSION && header.height() <= DETAIL_MAX_DIMENSION) {
                Files.write(target, bytes);
            } else {
                writeCapped(decode(bytes), bytes, DETAIL_MAX_DIMENSION, 0.9, target);
            }
            return filename;
        } catch (IOException e) {
            log.error("소개글 이미지 저장 실패: {}", originalFilename, e);
            deleteQuietly(target);
            throw new BusinessException(ConferenceErrorCode.IMAGE_UPLOAD_FAILED);
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
    }

    // ImageIO.read()로 곧장 디코딩하면 파일 용량 제한(10MB)과 무관하게 압축률이 높은 이미지가
    // 거대한 픽셀 버퍼를 할당시켜 메모리를 고갈시킬 수 있다(압축폭탄) - ImageReader로 헤더의
    // 가로*세로만 먼저 읽어 상한을 넘으면 실제 디코딩 전에 거부한다.
    private ImageHeader readImageHeader(byte[] bytes) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                throw new BusinessException(ConferenceErrorCode.IMAGE_INVALID_TYPE);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new BusinessException(ConferenceErrorCode.IMAGE_INVALID_TYPE);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((long) width * height > MAX_PIXELS) {
                    throw new BusinessException(ConferenceErrorCode.IMAGE_DIMENSIONS_TOO_LARGE);
                }
                String extension = CANONICAL_EXTENSION_BY_FORMAT.get(reader.getFormatName().toLowerCase(Locale.ROOT));
                if (extension == null) {
                    throw new BusinessException(ConferenceErrorCode.IMAGE_INVALID_TYPE);
                }
                return new ImageHeader(width, height, extension);
            } finally {
                reader.dispose();
            }
        }
    }

    private record ImageHeader(int width, int height, String extension) {
    }

    private BufferedImage decode(byte[] bytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new BusinessException(ConferenceErrorCode.IMAGE_INVALID_TYPE);
        }
        return image;
    }

    // Thumbnailator의 size()는 원본이 목표보다 작아도 기본적으로 확대해버린다 - 트래픽 절약이 목적인데
    // 오히려 용량만 커지고 화질도 흐려지므로, 원본이 이미 상한선 이내면 리사이징 없이 원본 바이트를 그대로 저장한다.
    private void writeCapped(BufferedImage original, byte[] originalBytes, int maxDimension, double quality, Path target) throws IOException {
        if (original.getWidth() <= maxDimension && original.getHeight() <= maxDimension) {
            Files.write(target, originalBytes);
            return;
        }
        Thumbnails.of(original)
                .size(maxDimension, maxDimension)
                .keepAspectRatio(true)
                .outputQuality(quality)
                .toFile(target.toFile());
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 저장 실패 시 정리 목적이라 삭제 실패는 무시한다.
        }
    }

    private String cleanOriginalFilename(MultipartFile file) {
        // getFileName()으로 경로 구분자를 전부 제거해 "../../etc/passwd" 같은 원본 파일명이 들어와도
        // 순수 파일명만 남긴다 - rootDir 밖으로 쓰기가 불가능해진다.
        return Paths.get(StringUtils.cleanPath(
                        StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file"))
                .getFileName().toString();
    }

    private void validateExtension(String filename, Set<String> allowedExtensions, ConferenceErrorCode errorCode) {
        int dotIndex = filename.lastIndexOf('.');
        String extension = dotIndex >= 0 ? filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT) : "";
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(errorCode);
        }
    }

    private Path resolveWithinRoot(Path root, String storedFilename, ConferenceErrorCode errorCodeIfEscapes) {
        Path target = root.resolve(storedFilename).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(errorCodeIfEscapes);
        }
        return target;
    }

    public Resource loadAsResource(String storedFilename) {
        return loadAsResource(proofRootDir, storedFilename, ConferenceErrorCode.PROOF_FILE_NOT_FOUND);
    }

    public Resource loadImageAsResource(String storedFilename) {
        return loadAsResource(imageRootDir, storedFilename, ConferenceErrorCode.IMAGE_NOT_FOUND);
    }

    // AI 요약 생성용으로 이미지 바이트를 직접 읽어야 할 때 쓴다(Gemini에 base64로 실어 보내기 위함).
    public byte[] loadImageBytes(String storedFilename) {
        Path path = resolveWithinRoot(imageRootDir, storedFilename, ConferenceErrorCode.IMAGE_NOT_FOUND);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BusinessException(ConferenceErrorCode.IMAGE_NOT_FOUND);
        }
    }

    // 저장 파일명은 항상 디코딩된 실제 포맷을 근거로 지어지므로(storeImage/storeDescriptionImage),
    // 확장자만 보고 mimeType을 판별해도 사용자가 올린 원본 확장자와 실제 포맷이 어긋날 위험이 없다.
    public String resolveImageMimeType(String storedFilename) {
        return storedFilename.toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg";
    }

    private Resource loadAsResource(Path root, String storedFilename, ConferenceErrorCode notFoundErrorCode) {
        try {
            Path filePath = resolveWithinRoot(root, storedFilename, notFoundErrorCode);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(notFoundErrorCode);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BusinessException(notFoundErrorCode);
        }
    }

    // 같은 등록 신청 안에서 증빙 파일은 저장됐는데 뒤이은 이미지 저장이 실패해 트랜잭션이 롤백될 때,
    // DB에는 남지 않는데 디스크에만 남는 orphan 증빙 파일을 정리하기 위해 호출한다.
    public void deleteProofFile(String storedFilename) {
        deleteQuietly(resolveWithinRoot(proofRootDir, storedFilename, ConferenceErrorCode.PROOF_FILE_UPLOAD_FAILED));
    }

    public String extractOriginalFilename(String storedFilename) {
        int separatorIndex = storedFilename.indexOf('_');
        return separatorIndex >= 0 && separatorIndex < storedFilename.length() - 1
                ? storedFilename.substring(separatorIndex + 1)
                : storedFilename;
    }

    public record StoredImage(String thumbnailFilename, String detailFilename) {
    }
}
