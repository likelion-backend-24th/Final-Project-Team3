package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.common.file.FileStorageService;
import com.example.conferenceservice.conference.client.ConferenceContentSummaryLlmClient;
import com.example.conferenceservice.conference.client.ConferenceContentSummaryLlmClient.ImagePart;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// 소개글을 저장(등록/수정)하는 시점에 소개글 텍스트+이미지를 분석해 AI 요약을 만들어둔다.
// 조회 시점에 매번 다시 생성하지 않는다 - 소개글이 바뀌지 않는 한 요약도 바뀔 이유가 없기 때문.
@Slf4j
@Service
@RequiredArgsConstructor
public class ConferenceContentSummaryService {

    private final ConferenceContentSummaryLlmClient llmClient;
    private final FileStorageService fileStorageService;

    // DescriptionEditor(프론트)가 소개글 이미지 삽입 자체를 1장으로 막아두지만, 서버도 같은 규칙을
    // 다시 검증한다(요청을 직접 조작해서 프론트 제한을 우회하는 경우 방지).
    public void validateImageLimit(String description) {
        if (DescriptionMarkdownImages.countImages(description) > 1) {
            throw new BusinessException(ConferenceErrorCode.DESCRIPTION_IMAGE_LIMIT_EXCEEDED);
        }
    }

    // AI 요약 생성 실패(LLM 호출 실패, 이미지 파일 누락 등)는 전부 이 메서드 안에서 흡수한다 -
    // 호출부(컨퍼런스 등록·소개글 수정)는 AI 요약과 무관하게 항상 성공해야 하기 때문.
    public void generateAndAttach(Conference conference) {
        try {
            String description = conference.getDescription();
            List<ImagePart> images = collectImages(conference, description);
            if ((description == null || description.isBlank()) && images.isEmpty()) {
                return;
            }
            String summary = llmClient.generateSummary(description, images);
            conference.updateAiSummary(summary);
        } catch (RuntimeException e) {
            log.warn("컨퍼런스 AI 요약 생성 실패: conferenceId={}", conference.getId(), e);
        }
    }

    // 대표 배너 이미지(썸네일) + 소개글 본문에서 추출한 이미지 1장, 합쳐서 최대 2장만 Gemini에 보낸다.
    private List<ImagePart> collectImages(Conference conference, String description) {
        List<ImagePart> images = new ArrayList<>();
        addImageIfPresent(images, conference.getThumbnailImageName());
        addImageIfPresent(images, DescriptionMarkdownImages.firstStoredImageFilename(description));
        return images;
    }

    // 이미지 하나를 못 읽어도(파일 누락 등) 소개글 텍스트만으로는 여전히 요약이 가능하므로,
    // 여기서 실패를 삼켜 이 이미지만 제외하고 넘어간다 - generateAndAttach 전체를 포기하지 않는다.
    private void addImageIfPresent(List<ImagePart> images, String storedFilename) {
        if (storedFilename == null) {
            return;
        }
        try {
            byte[] bytes = fileStorageService.loadImageBytes(storedFilename);
            images.add(new ImagePart(bytes, fileStorageService.resolveImageMimeType(storedFilename)));
        } catch (RuntimeException e) {
            log.warn("이미지 로드 실패로 AI 요약 분석 대상에서 제외합니다: storedFilename={}", storedFilename, e);
        }
    }
}
