package com.example.conferenceservice.conference.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
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
        // SimpleClientHttpRequestFactory(HttpURLConnection 기반)는 keep-alive 커넥션 재사용 시
        // 가끔 본문이 빈 응답을 그대로 돌려줘서(Content-Type 없음 -> octet-stream으로 오인) JSON
        // 파싱이 실패하는 문제가 있었다 - JDK 11+ HttpClient 기반으로 바꿔서 이 문제를 없앤다.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

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

    // 소개글이 짧거나 더미 텍스트에 가까우면 모델이 "요약" 대신 옵션 제시·재입력 요청 같은
    // 메타 답변으로 새는 경우가 있었다(예: "**옵션 1** ... **옵션 2** ... 다시 알려주세요") -
    // 저장되는 aiSummary는 항상 참가자에게 그대로 노출되는 문장이라 이런 이탈을 명시적으로 금지한다.
    private String buildPrompt(String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 참가자에게 공개될 컨퍼런스 소개글입니다.\n");
        sb.append("소개글: ").append(description == null || description.isBlank() ? "(작성되지 않음)" : description).append("\n");
        sb.append("\n첨부된 이미지가 있다면 함께 참고해서, 참가자가 이 컨퍼런스가 어떤 자리인지 한눈에 알 수 있도록 ");
        sb.append("자연스러운 한국어 1~2문장으로 요약해주세요. 소개글 문장을 그대로 반복하지 말고 핵심만 요약하세요.\n\n");
        sb.append("반드시 지킬 것:\n");
        sb.append("- 완성된 요약 문장만 출력하고, 그 외의 말은 절대 덧붙이지 않는다.\n");
        sb.append("- 선택지(옵션)를 제시하거나, 입력이 부족·부적절하다고 지적하거나, 추가 정보를 요청하는 문장을 절대 쓰지 않는다.\n");
        sb.append("- 소개글이 짧거나 모호해도 주어진 내용만으로 최선의 요약을 만들어낸다.\n");
        sb.append("- 글머리 기호, 굵게, 인용 같은 마크다운 문법 없이 순수한 문장으로만 답한다.");
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
