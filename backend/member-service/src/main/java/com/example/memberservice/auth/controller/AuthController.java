package com.example.memberservice.auth.controller;

import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.auth.dto.LoginResponse;
import com.example.memberservice.auth.dto.SocialLinkRequest;
import com.example.memberservice.auth.dto.SocialLoginRequest;
import com.example.memberservice.auth.entity.SocialProvider;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.security.CookieProvider;
import com.example.memberservice.auth.security.CustomUserDetails;
import com.example.memberservice.auth.service.AuthService;
import com.example.memberservice.common.TraceIdProvider;
import com.example.memberservice.common.dto.ApiResponse;
import com.example.memberservice.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;
    private final TraceIdProvider traceIdProvider;
    private final CookieProvider cookieProvider;

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 Access Token을 발급받는다. Refresh Token은 HttpOnly 쿠키로 별도 발급됨.")
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

    @Operation(summary = "소셜 로그인", description = "Google/Kakao 인증 토큰으로 로그인/최초가입 처리하고 Access Token을 발급받는다. 참석자(MEMBER) 전용 — ORGANIZER 권한은 발급되지 않는다.")
    @PostMapping("/social/{provider}")
    public ResponseEntity<ApiResponse<LoginResponse>> socialLogin(
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthService.AuthTokens tokens = authService.socialLogin(
                parseProvider(provider), request.token(), request.redirectUri(), request.ageGroup(), request.job()
        );
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(tokens.refreshToken()).toString())
                .body(ApiResponse.success("소셜 로그인에 성공했습니다.", tokens.body(), traceId));
    }

    @Operation(summary = "소셜 계정 연동", description = "로그인된 본인 계정에 소셜 계정을 연동한다. 소셜 인증 이메일이 본인 계정 이메일과 일치해야 한다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/social/{provider}/link")
    public ResponseEntity<ApiResponse<Void>> linkSocialAccount(
            @PathVariable String provider,
            @Valid @RequestBody SocialLinkRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        authService.linkSocialAccount(currentUser.getMemberId(), parseProvider(provider), request.token(), request.redirectUri());
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok(ApiResponse.success("소셜 계정이 연동되었습니다.", null, traceId));
    }

    @Operation(summary = "토큰 재발급", description = "refreshToken 쿠키로 Access/Refresh Token 쌍을 새로 발급받는다 (Rotation: 기존 Refresh Token은 즉시 폐기).")
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

    @Operation(summary = "로그아웃", description = "제출된 Refresh Token을 폐기하고 refreshToken 쿠키를 삭제한다.")
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

    private SocialProvider parseProvider(String provider) {
        try {
            return SocialProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.SOCIAL_PROVIDER_UNSUPPORTED);
        }
    }
}
