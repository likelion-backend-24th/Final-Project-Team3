package com.example.conferenceservice.common.file;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// EC2 단일 서버 + docker compose 배포 구조라 로컬 디스크에 저장한다.
// infra/compose.yaml에서 이 경로를 호스트 볼륨으로 마운트해야 재배포 시에도 파일이 유지된다.
@Slf4j
@Service
public class FileStorageService {

    private final Path rootDir;

    public FileStorageService(@Value("${app.upload.conference-proof-dir}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + rootDir, e);
        }
    }

    public String store(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(
                StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file");
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        try {
            Path target = rootDir.resolve(storedFilename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return storedFilename;
        } catch (IOException e) {
            log.error("증명 파일 저장 실패: {}", originalFilename, e);
            throw new BusinessException(ConferenceErrorCode.PROOF_FILE_UPLOAD_FAILED);
        }
    }

    public Resource loadAsResource(String storedFilename) {
        try {
            Path filePath = rootDir.resolve(storedFilename).normalize();
            if (!filePath.startsWith(rootDir)) {
                throw new BusinessException(ConferenceErrorCode.PROOF_FILE_NOT_FOUND);
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ConferenceErrorCode.PROOF_FILE_NOT_FOUND);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BusinessException(ConferenceErrorCode.PROOF_FILE_NOT_FOUND);
        }
    }

    public String extractOriginalFilename(String storedFilename) {
        int separatorIndex = storedFilename.indexOf('_');
        return separatorIndex >= 0 && separatorIndex < storedFilename.length() - 1
                ? storedFilename.substring(separatorIndex + 1)
                : storedFilename;
    }
}
