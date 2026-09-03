package com.example.conferenceservice.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenValidator {

    private final SecretKey key;

    public JwtTokenValidator(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims validate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException(e);
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(Throwable cause) {
            super("유효하지 않은 토큰입니다.", cause);
        }
    }
}
