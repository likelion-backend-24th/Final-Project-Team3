package com.example.memberservice.auth.service;

import com.example.memberservice.auth.entity.RefreshToken;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.repository.RefreshTokenRepository;
import com.example.memberservice.common.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static com.example.memberservice.common.HashUtil.sha256;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenRevoker refreshTokenRevoker;

    @Getter
    @Value("${jwt.refresh-token-validity-ms}")
    private long validityMs;

    @Transactional
    public String issue(UUID memberId) {
        String rawToken = generateRawToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(validityMs * 1_000_000);

        RefreshToken refreshToken = RefreshToken.issue(memberId, sha256(rawToken), expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken found = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (found.isExpired(LocalDateTime.now())) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        int revokedRows = refreshTokenRepository.revokeIfActive(found.getId());
        if (revokedRows == 0) {
            // 이미 폐기된 토큰이 다시 제출됨 -> 재사용(탈취) 의심, 동시 레이스에서 진 경우도 여기로 들어옴
            refreshTokenRevoker.revokeAll(found.getMemberId());
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_REUSED);
        }

        String newRawToken = issue(found.getMemberId());

        return new RotationResult(found.getMemberId(), newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .ifPresent(RefreshToken::revoke);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RotationResult(UUID memberId, String newRefreshToken) {
    }
}
