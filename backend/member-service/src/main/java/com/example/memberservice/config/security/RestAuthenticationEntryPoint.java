package com.example.memberservice.config.security;

import com.example.memberservice.common.TraceIdProvider;
import com.example.memberservice.common.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

// 인증 실패 시 응답 형식 지정
// JwtAuthenticationFilter에서 토큰이 없거나 잘못돼서 SecurityContext가 비어있는 상태로 anyRequest().authenticated()가 적용된 URL에 도달하면,
// Spring Security가 자동으로 401을 내리긴 하는데 우리 프로젝트의 ApiResponse 포맷이 아님.
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final TraceIdProvider traceIdProvider;
    private final ObjectMapper objectMapper;

    // Spring Security가 인증 안 된 사람이 보호된 URL에 접근했다고 판단하면 이 메서드를 호출
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ApiResponse<Void> body = ApiResponse.error("AUTH_REQUIRED", "인증이 필요합니다.", traceIdProvider.resolve(request));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        objectMapper.writeValue(response.getWriter(), body);
    }


}
