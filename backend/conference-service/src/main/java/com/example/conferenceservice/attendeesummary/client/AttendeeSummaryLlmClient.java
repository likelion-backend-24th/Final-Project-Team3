package com.example.conferenceservice.attendeesummary.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
            // 이 모델은 기본적으로 내부 추론(thinking)을 하는데, 단순 1~2문장 요약엔 불필요하게 토큰을 많이 써서
            // (thinkingBudget 미지정 시 수백~천 토큰 이상) maxOutputTokens를 다 소진하고 답변이 잘리는 문제가 있었음
            // -> thinkingBudget을 0으로 꺼서 실제 답변 생성에만 토큰을 쓰도록 함
            GeminiRequest request = new GeminiRequest(
                    List.of(new Content(List.of(new Part(buildPrompt(ageGroupDistribution, jobDistribution, reviews))))),
                    new GenerationConfig(512, new ThinkingConfig(0)));

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

    // Gemini는 답변을 여러 part로 나눠 보낼 수 있어서 첫 part만 읽으면 문장이 중간에 잘린 채 저장됐다
    // ("이번 컨"처럼) -> 추론(thought) part를 뺀 텍스트 part를 전부 이어 붙인다.
    // 정상 종료(STOP)가 아니거나(토큰 한도·안전 필터 등) 내용이 비면 잘린 문장을 저장하지 않도록 실패로 본다.
    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new AttendeeSummaryLlmException("Gemini 응답에 candidates가 없습니다", null);
        }
        Candidate candidate = response.candidates().get(0);
        if (candidate.finishReason() != null && !"STOP".equals(candidate.finishReason())) {
            throw new AttendeeSummaryLlmException("Gemini 응답이 정상 종료되지 않았습니다: finishReason=" + candidate.finishReason(), null);
        }
        List<ResponsePart> parts = candidate.content() == null ? null : candidate.content().parts();
        String text = parts == null ? "" : parts.stream()
                .filter(part -> !Boolean.TRUE.equals(part.thought()))
                .map(ResponsePart::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining())
                .trim();
        if (text.isEmpty()) {
            throw new AttendeeSummaryLlmException("Gemini 응답에 요약 텍스트가 없습니다", null);
        }
        return text;
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
    private record GenerationConfig(int maxOutputTokens, ThinkingConfig thinkingConfig) {}
    private record ThinkingConfig(int thinkingBudget) {}
    private record Content(List<Part> parts) {}
    private record Part(String text) {}

    private record GeminiResponse(List<Candidate> candidates) {}
    private record Candidate(ResponseContent content, String finishReason) {}
    private record ResponseContent(List<ResponsePart> parts) {}
    private record ResponsePart(String text, Boolean thought) {}
}
