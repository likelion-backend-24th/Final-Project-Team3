package com.example.gateway;

import com.sun.net.httpserver.HttpHandler;
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
public class MemberRouteTest {

    private static final HttpServer STUB_SERVER = createStubServer();
    private static final AtomicReference<List<String>> RECEIVED_TRACE_IDS = new AtomicReference<>();
    private static final AtomicReference<List<String>> RECEIVED_AUTH_HEADERS = new AtomicReference<>();

    @Autowired
    RestTestClient restTestClient;

    private static HttpServer createStubServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            HttpHandler stubHandler = exchange -> {
                RECEIVED_TRACE_IDS.set(exchange.getRequestHeaders().get("X-Trace-Id"));
                RECEIVED_AUTH_HEADERS.set(exchange.getRequestHeaders().get("Authorization"));
                byte[] body = "{}".getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            };
            // member-service는 /api/auth/**, /api/members/** 두 경로 모두 하나의 라우트로 묶여 있어서
            // stub 서버에도 두 컨텍스트를 모두 등록해 각각 전달되는지 확인한다.
            server.createContext("/api/auth", stubHandler);
            server.createContext("/api/members", stubHandler);
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void overrideMemberServiceUrl(DynamicPropertyRegistry registry) {
        int port = STUB_SERVER.getAddress().getPort();
        registry.add("services.member-service.url", () -> "http://localhost:" + port);
    }

    @AfterAll
    static void stopStubServer() {
        STUB_SERVER.stop(0);
    }

    @Test
    void 게이트웨이가_auth_요청을_memberService로_전달한다() {
        restTestClient.get()
                .uri("/api/auth/login")
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().is2xxSuccessful();

        assertThat(RECEIVED_TRACE_IDS.get()).hasSize(1);
        assertThat(RECEIVED_TRACE_IDS.get().get(0)).isNotBlank();

        assertThat(RECEIVED_AUTH_HEADERS.get()).containsExactly("Bearer test-token");
    }

    @Test
    void 게이트웨이가_members_요청을_memberService로_전달한다() {
        restTestClient.get()
                .uri("/api/members/me")
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
                .uri("/api/auth/login")
                .header("X-Trace-Id", "spoofed-trace-id")
                .exchange()
                .expectStatus().is2xxSuccessful();

        assertThat(RECEIVED_TRACE_IDS.get()).hasSize(1);
        assertThat(RECEIVED_TRACE_IDS.get().get(0)).isNotEqualTo("spoofed-trace-id");
    }
}
