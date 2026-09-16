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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reservation-Service가 실제로 5xx를 반환할 때 PaymentSummaryClient가 이를
 * PaymentSummaryUnavailableException으로 정상 변환하는지 검증한다 (Task 17-3 통합 확인).
 * 기존 단위 테스트는 MockitoBean으로 예외를 직접 던지게 했지만, 이 테스트는 실제 HTTP 5xx
 * 응답이 RestClientException 경로를 거쳐 올바르게 변환되는지까지 확인한다.
 */
@SpringBootTest
class PaymentSummaryClientFailureIntegrationTest {

    private static HttpServer server;

    @Autowired
    private PaymentSummaryClient paymentSummaryClient;

    @DynamicPropertySource
    static void overrideReservationServiceUrl(DynamicPropertyRegistry registry) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/internal/sessions/payment-summary", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        registry.add("services.reservation-service.url", () -> "http://localhost:" + server.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void reservationService가_500을_반환하면_PaymentSummaryUnavailableException으로_변환된다() {
        List<UUID> sessionIds = List.of(UUID.randomUUID());

        assertThatThrownBy(() -> paymentSummaryClient.getPaymentSummary(sessionIds))
                .isInstanceOf(PaymentSummaryUnavailableException.class);
    }
}
