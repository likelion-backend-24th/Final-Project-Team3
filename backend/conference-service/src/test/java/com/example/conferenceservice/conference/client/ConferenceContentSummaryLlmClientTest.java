package com.example.conferenceservice.conference.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 Gemini 대신 로컬 HttpServer로 응답 본문을 흉내 내서, 응답 파싱 규칙만 검증한다
class ConferenceContentSummaryLlmClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private ConferenceContentSummaryLlmClient clientRespondingWith(String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new ConferenceContentSummaryLlmClient(RestClient.builder(), baseUrl, "test-key", "test-model");
    }

    @Test
    @DisplayName("답변이 여러 part로 나뉘어 와도 전부 이어 붙여 돌려준다")
    void 여러_part를_이어_붙인다() throws IOException {
        ConferenceContentSummaryLlmClient client = clientRespondingWith("""
                {"candidates":[{"content":{"parts":[{"text":"실무 LLM "},{"text":"노하우를 나누는 자리입니다."}]},"finishReason":"STOP"}]}
                """);

        assertThat(client.generateSummary("소개글", List.of())).isEqualTo("실무 LLM 노하우를 나누는 자리입니다.");
    }

    @Test
    @DisplayName("정상 종료되지 않은 응답은 잘린 요약을 저장하지 않도록 실패로 처리한다")
    void 정상_종료가_아니면_실패로_본다() throws IOException {
        ConferenceContentSummaryLlmClient client = clientRespondingWith("""
                {"candidates":[{"content":{"parts":[{"text":"실무 LLM"}]},"finishReason":"MAX_TOKENS"}]}
                """);

        assertThatThrownBy(() -> client.generateSummary("소개글", List.of()))
                .isInstanceOf(ConferenceContentSummaryLlmException.class)
                .hasMessageContaining("MAX_TOKENS");
    }
}
