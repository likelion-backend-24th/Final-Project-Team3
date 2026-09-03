package com.example.memberservice.auth.controller;

import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.auth.dto.LoginResponse;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.security.CookieProvider;
import com.example.memberservice.auth.service.AuthService;
import com.example.memberservice.common.TraceIdProvider;
import com.example.memberservice.common.dto.ApiResponse;
import com.example.memberservice.common.exception.BusinessException;
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

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;
    private final TraceIdProvider traceIdProvider;
    private final CookieProvider cookieProvider;

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
        if (refreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_MISSING);
        }

        AuthService.AuthTokens tokens = authService.reissue(refreshToken);
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(tokens.refreshToken()).toString())
                .body(ApiResponse.success("토큰이 재발급되었습니다.", tokens.body(), traceId));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletRequest httpRequest
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieProvider.clearCookie(REFRESH_TOKEN_COOKIE).toString())
                .body(ApiResponse.success("로그아웃되었습니다.", null, traceId));
    }

    private ResponseCookie refreshTokenCookie(String refreshToken) {
        Duration maxAge = Duration.ofMillis(authService.getRefreshTokenValidityMs());
        return cookieProvider.createCookie(REFRESH_TOKEN_COOKIE, refreshToken, maxAge);
    }
}
