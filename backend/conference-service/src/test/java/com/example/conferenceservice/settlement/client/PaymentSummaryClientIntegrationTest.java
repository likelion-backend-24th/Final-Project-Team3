package com.example.conferenceservice.settlement.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PaymentSummaryClient가 목(mock)이 아닌 실제 HTTP 응답(JSON 역직렬화 포함)을 정상적으로
 * 처리하는지 검증한다 (Task 17-3 통합 확인). Reservation-Service의 실제 ApiResponse
 * 포맷(success/message/data/traceId)을 그대로 재현한 로컬 서버를 띄워 검증하며,
 * ConferenceSettlementAcceptanceTest처럼 클라이언트 자체를 MockitoBean으로 대체하지 않는다.
 */
@SpringBootTest
class PaymentSummaryClientIntegrationTest {

    private static HttpServer server;
    private static final AtomicReference<String> lastQuery = new AtomicReference<>();

    @Autowired
    private PaymentSummaryClient paymentSummaryClient;

    @DynamicPropertySource
    static void overrideReservationServiceUrl(DynamicPropertyRegistry registry) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/internal/sessions/payment-summary", exchange -> {
            lastQuery.set(exchange.getRequestURI().getQuery());
            String body = "{\"success\":true,\"message\":\"정산 집계 조회 완료\","
                    + "\"data\":{\"totalRevenue\":100000,\"refundedAmount\":20000,\"netRevenue\":80000,"
                    + "\"confirmedCount\":10,\"cancelledCount\":2},\"traceId\":\"test-trace-id\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        registry.add("services.reservation-service.url", () -> "http://localhost:" + server.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void 실제_HTTP_응답을_정상적으로_역직렬화한다() {
        UUID sessionId = UUID.randomUUID();

        PaymentSummaryClient.PaymentSummaryResponse response = paymentSummaryClient.getPaymentSummary(List.of(sessionId));

        assertThat(response.totalRevenue()).isEqualTo(100000);
        assertThat(response.refundedAmount()).isEqualTo(20000);
        assertThat(response.netRevenue()).isEqualTo(80000);
        assertThat(response.confirmedCount()).isEqualTo(10);
        assertThat(response.cancelledCount()).isEqualTo(2);
        assertThat(lastQuery.get()).contains(sessionId.toString());
    }
}
