package com.example.conferenceservice.config.security;

import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final TraceIdProvider traceIdProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ApiResponse<Void> body = ApiResponse.error("AUTH_REQUIRED", "인증이 필요합니다.", traceIdProvider.resolve(request));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
