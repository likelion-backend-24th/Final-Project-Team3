package com.example.conferenceservice.attendeesummary.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class AttendeeSummaryLlmClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AttendeeSummaryLlmClient(RestClient.Builder builder,
                                    @Value("${gemini.api-base-url}") String baseUrl,
                                    @Value("${gemini.api-key}") String apiKey,
                                    @Value("${llm.summary.attendee-stats.model}") String model) {
        // 공용 RestClientConfig의 기본 timeout(연결 500ms/응답 1000ms)은 내부 서비스 간 호출 기준이라
        // 외부 LLM API 호출엔 너무 짧음 -> 이 클라이언트만 별도로 넉넉하게 재설정
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

    public String generateSummary(Map<String, Long> ageGroupDistribution,
                                  Map<String, Long> jobDistribution,
                                  List<String> reviews) {
        try {
            GeminiRequest request = new GeminiRequest(
                    List.of(new Content(List.of(new Part(buildPrompt(ageGroupDistribution, jobDistribution, reviews))))),
                    new GenerationConfig(300));

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
            // 네트워크 오류·타임아웃·4xx/5xx 전부 여기로 모여서 서비스 계층은 이 예외 하나만 처리하면 됨
            throw new AttendeeSummaryLlmException("Gemini API 호출 실패", e);
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new AttendeeSummaryLlmException("Gemini 응답에 candidates가 없습니다", null);
        }
        return response.candidates().get(0).content().parts().get(0).text();
    }

    private String buildPrompt(Map<String, Long> ageGroupDistribution,
                               Map<String, Long> jobDistribution,
                               List<String> reviews) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 컨퍼런스 체크인 참석자 통계입니다.\n");
        sb.append("연령대 분포: ").append(ageGroupDistribution).append("\n");
        sb.append("직무 분포: ").append(jobDistribution).append("\n");
        if (!reviews.isEmpty()) {
            sb.append("참석자 후기:\n");
            reviews.forEach(r -> sb.append("- ").append(r).append("\n"));
        }
        sb.append("\n위 정보를 바탕으로 주최자에게 보여줄 참석자 요약을 자연스러운 한국어 1~2문장으로 작성해주세요. 수치를 그대로 나열하지 말고 특징을 요약하세요.");
        return sb.toString();
    }

    // ---- Gemini 요청/응답 DTO ----
    private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {}
    private record GenerationConfig(int maxOutputTokens) {}
    private record Content(List<Part> parts) {}
    private record Part(String text) {}

    private record GeminiResponse(List<Candidate> candidates) {}
    private record Candidate(Content content) {}
}
