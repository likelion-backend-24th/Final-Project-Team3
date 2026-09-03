package com.example.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class ConferenceRouteTest {

    private static final HttpServer STUB_SERVER = createStubServer();
    private static final AtomicReference<List<String>> RECEIVED_TRACE_IDS = new AtomicReference<>();
    private static final AtomicReference<List<String>> RECEIVED_AUTH_HEADERS = new AtomicReference<>();

    @Autowired
    RestTestClient restTestClient;

    private static HttpServer createStubServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/conferences", exchange -> {
                RECEIVED_TRACE_IDS.set(exchange.getRequestHeaders().get("X-Trace-Id"));
                RECEIVED_AUTH_HEADERS.set(exchange.getRequestHeaders().get("Authorization"));
                byte[] body = "[]".getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void overrideConferenceServiceUrl(DynamicPropertyRegistry registry) {
        int port = STUB_SERVER.getAddress().getPort();
        registry.add("services.conference-service.url", () -> "http://localhost:" + port);
    }

    @AfterAll
    static void stopStubServer() {
        STUB_SERVER.stop(0);
    }

    @Test
    void 게이트웨이가_요청을_conferenceService로_전달한다() {
        restTestClient.get()
                .uri("/api/conferences")
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().is2xxSuccessful();

        assertThat(RECEIVED_TRACE_IDS.get()).hasSize(1);
        assertThat(RECEIVED_TRACE_IDS.get().get(0)).isNotBlank();

        assertThat(RECEIVED_AUTH_HEADERS.get()).containsExactly("Bearer test-token");
    }

    @Test
    void 클라이언트가_보낸_Trace_Id는_Gateway가_새로_발급한_값으로_덮어쓴다() {
        restTestClient.get()
                .uri("/api/conferences")
                .header("X-Trace-Id", "spoofed-trace-id")
                .exchange()
                .expectStatus().is2xxSuccessful();

        assertThat(RECEIVED_TRACE_IDS.get()).hasSize(1);
        assertThat(RECEIVED_TRACE_IDS.get().get(0)).isNotEqualTo("spoofed-trace-id");
    }
}
