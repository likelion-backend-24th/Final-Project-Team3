package com.example.conferenceservice.conference.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

// AttendeeSummaryLlmClient(참석자 통계 - 텍스트 전용)와 별개 클라이언트다: 이 클라이언트는 소개글 텍스트에
// 이미지(대표 배너 + 소개글 본문 이미지, 최대 2장)를 함께 실어 보내는 멀티모달 요청을 다룬다.
@Component
public class ConferenceContentSummaryLlmClient {

    private static final int MAX_OUTPUT_TOKENS = 512;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public ConferenceContentSummaryLlmClient(RestClient.Builder builder,
                                              @Value("${gemini.api-base-url}") String baseUrl,
                                              @Value("${gemini.api-key}") String apiKey,
                                              @Value("${llm.summary.content.model}") String model) {
        // 참석자 요약 클라이언트와 같은 이유로 공용 기본 timeout보다 넉넉하게 재설정한다.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(5000);

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generateSummary(String description, List<ImagePart> images) {
        try {
            GeminiRequest request = new GeminiRequest(
                    List.of(new Content(buildParts(description, images))),
                    new GenerationConfig(MAX_OUTPUT_TOKENS, new ThinkingConfig(0)));

            GeminiResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            return extractText(response);
        } catch (RestClientException e) {
            throw new ConferenceContentSummaryLlmException("Gemini API 호출 실패", e);
        }
    }

    private List<Part> buildParts(String description, List<ImagePart> images) {
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(buildPrompt(description), null));
        for (ImagePart image : images) {
            parts.add(new Part(null, new InlineData(image.mimeType(), Base64.getEncoder().encodeToString(image.bytes()))));
        }
        return parts;
    }

    private String buildPrompt(String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 참가자에게 공개될 컨퍼런스 소개글입니다.\n");
        sb.append("소개글: ").append(description == null || description.isBlank() ? "(작성되지 않음)" : description).append("\n");
        sb.append("\n첨부된 이미지가 있다면 함께 참고해서, 참가자가 이 컨퍼런스가 어떤 자리인지 한눈에 알 수 있도록 ");
        sb.append("자연스러운 한국어 1~2문장으로 요약해주세요. 소개글 문장을 그대로 반복하지 말고 핵심만 요약하세요.");
        return sb.toString();
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new ConferenceContentSummaryLlmException("Gemini 응답에 candidates가 없습니다", null);
        }
        return response.candidates().get(0).content().parts().get(0).text();
    }

    public record ImagePart(byte[] bytes, String mimeType) {
    }

    // ---- Gemini 요청/응답 DTO ----
    private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {}
    private record GenerationConfig(int maxOutputTokens, ThinkingConfig thinkingConfig) {}
    private record ThinkingConfig(int thinkingBudget) {}
    private record Content(List<Part> parts) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Part(String text, InlineData inlineData) {}
    private record InlineData(String mimeType, String data) {}

    private record GeminiResponse(List<Candidate> candidates) {}
    private record Candidate(Content content) {}
}
