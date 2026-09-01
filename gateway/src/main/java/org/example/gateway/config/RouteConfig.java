package org.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.UUID;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
public class RouteConfig {

    @Value("${services.conference-service.url}")
    private String conferenceServiceUrl;

    @Bean
    public RouterFunction<ServerResponse> conferenceServiceRoute() {
        return route("conference-service")  // 라우트 이름
                .route(org.springframework.web.servlet.function.RequestPredicates.path("/api/conferences/**"), http()) // 이 경로로 오는 요청은 그냥 그대로 전달
                .before(uri(conferenceServiceUrl)) // 전달할 대상 서버 주소 지정 (application.yaml의 services.conference-service.url 값)
                .before(this::addTraceId) // 요청이 실제로 전달되기 전에 X-Trace-Id 헤더를 매번 새로 생성해서 붙임
                .before(this::passAuthHeader)
                .build();
    }

    private ServerRequest addTraceId(ServerRequest request) {
        String traceId = UUID.randomUUID().toString();
        return ServerRequest.from(request)
                .header("X-Trace-Id", traceId)
                .build();
    }

    private ServerRequest passAuthHeader(ServerRequest request) {
        String authHeader = request.headers().firstHeader("Authorization");
        if (authHeader == null) {
            return request;
        }
        return ServerRequest.from(request)
                .header("Authorization", authHeader)
                .build();
    }
}
