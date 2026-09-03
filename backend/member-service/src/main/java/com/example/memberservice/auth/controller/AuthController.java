package com.example.memberservice.auth.controller;

import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.auth.dto.LoginResponse;
import com.example.memberservice.auth.service.AuthService;
import com.example.memberservice.common.TraceIdProvider;
import com.example.memberservice.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;
    private final TraceIdProvider traceIdProvider;

    @Value("${jwt.refresh-token-validity-ms}")
    private long refreshTokenValidityMs;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthService.AuthTokens tokens = authService.login(request);
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(tokens.refreshToken()).toString())
                .body(ApiResponse.success("로그인에 성공했습니다.", tokens.body(), traceId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue(REFRESH_TOKEN_COOKIE) String refreshToken,
            HttpServletRequest httpRequest
    ) {
        AuthService.AuthTokens tokens = authService.reissue(refreshToken);
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(tokens.refreshToken()).toString())
                .body(ApiResponse.success("토큰이 재발급되었습니다.", tokens.body(), traceId));
    }

    private ResponseCookie refreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                // 프론트가 다른 오리진(포트)에서 fetch로 호출하는 걸 가정 — Lax/Strict면 크로스 오리진 요청에 쿠키가 안 실림
                .sameSite("None")
                .path("/api/auth")
                .maxAge(refreshTokenValidityMs / 1000)
                .build();
    }
}
