package com.example.conferenceservice.attendeesummary.client;

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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 Gemini 대신 로컬 HttpServer로 응답 본문을 흉내 내서, 응답 파싱 규칙만 검증한다
class AttendeeSummaryLlmClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private AttendeeSummaryLlmClient clientRespondingWith(String body) throws IOException {
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
        return new AttendeeSummaryLlmClient(RestClient.builder(), baseUrl, "test-key", "test-model");
    }

    private String generate(AttendeeSummaryLlmClient client) {
        return client.generateSummary(Map.of("TWENTIES", 3L), Map.of("DEVELOPER", 3L), List.of("좋았어요"));
    }

    @Test
    @DisplayName("답변이 여러 part로 나뉘어 와도 전부 이어 붙여 한 문장으로 돌려준다")
    void 여러_part를_이어_붙인다() throws IOException {
        AttendeeSummaryLlmClient client = clientRespondingWith("""
                {"candidates":[{"content":{"parts":[{"text":"이번 컨"},{"text":"퍼런스는 개발자 중심이었습니다."}]},"finishReason":"STOP"}]}
                """);

        assertThat(generate(client)).isEqualTo("이번 컨퍼런스는 개발자 중심이었습니다.");
    }

    @Test
    @DisplayName("추론(thought) part는 요약에 포함하지 않는다")
    void thought_part는_제외한다() throws IOException {
        AttendeeSummaryLlmClient client = clientRespondingWith("""
                {"candidates":[{"content":{"parts":[{"text":"내부 추론","thought":true},{"text":"참석자 만족도가 높았습니다."}]},"finishReason":"STOP"}]}
                """);

        assertThat(generate(client)).isEqualTo("참석자 만족도가 높았습니다.");
    }

    @Test
    @DisplayName("토큰 한도 등으로 정상 종료되지 않은 응답은 잘린 문장을 돌려주지 않고 실패로 처리한다")
    void 정상_종료가_아니면_실패로_본다() throws IOException {
        AttendeeSummaryLlmClient client = clientRespondingWith("""
                {"candidates":[{"content":{"parts":[{"text":"이번 컨"}]},"finishReason":"MAX_TOKENS"}]}
                """);

        assertThatThrownBy(() -> generate(client))
                .isInstanceOf(AttendeeSummaryLlmException.class)
                .hasMessageContaining("MAX_TOKENS");
    }

    @Test
    @DisplayName("텍스트가 비어 있는 응답은 실패로 처리한다")
    void 빈_응답은_실패로_본다() throws IOException {
        AttendeeSummaryLlmClient client = clientRespondingWith("""
                {"candidates":[{"content":{"parts":[]},"finishReason":"STOP"}]}
                """);

        assertThatThrownBy(() -> generate(client))
                .isInstanceOf(AttendeeSummaryLlmException.class);
    }
}
