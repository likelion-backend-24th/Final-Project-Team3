package com.example.memberservice.auth.security;

import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.common.exception.BusinessException;
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

    // JwtTokenProvider가 발급할 때 쓴 것과 같은 비밀키
    // 대칭키(HMAC) 방식이라 서명 키와 검증 키가 동일해야함
    private final SecretKey key;

    public JwtTokenValidator(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 서명 검증 + 만료시간 체크까지 Jwts 라이브러리가 알아서 해줌
    // 통과하면 토큰 안의 claims(내용물) 반환, 실패하면 예외
    public Claims validate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
    }


}
