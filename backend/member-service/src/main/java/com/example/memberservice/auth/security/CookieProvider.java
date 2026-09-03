package com.example.memberservice.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieProvider {
    private static final String PATH = "/api/auth";

    @Value("${cookie.secure}")
    private boolean secure;

    @Value("${cookie.same-site}")
    private String sameSite;

    // 프론트가 다른 오리진(포트)에서 fetch로 호출하는 걸 가정 — Lax/Strict면 크로스 오리진 요청에 쿠키가 안 실림
    public ResponseCookie createCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie clearCookie(String name) {
        return createCookie(name, "", Duration.ZERO);
    }
}
