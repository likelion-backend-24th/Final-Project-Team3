package org.example.gateway;

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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class ConferenceRouteTest {

    private static final HttpServer STUB_SERVER = createStubServer();
    private static final AtomicReference<String> RECEIVED_TRACE_ID = new AtomicReference<>();
    private static final AtomicReference<String> RECEIVED_AUTH_HEADER = new AtomicReference<>();

    @Autowired
    RestTestClient restTestClient;

    private static HttpServer createStubServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/conferences", exchange -> {
                RECEIVED_TRACE_ID.set(exchange.getRequestHeaders().getFirst("X-Trace-Id"));
                RECEIVED_AUTH_HEADER.set(exchange.getRequestHeaders().getFirst("Authorization"));
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

        assertThat(RECEIVED_TRACE_ID.get()).isNotBlank();
        assertThat(RECEIVED_AUTH_HEADER.get()).isEqualTo("Bearer test-token");
    }
}
