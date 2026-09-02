package com.example.memberservice.auth.service;

import com.example.memberservice.auth.entity.RefreshToken;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.repository.RefreshTokenRepository;
import com.example.memberservice.common.exception.BusinessException;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenRevoker refreshTokenRevoker;

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

        if (found.isRevoked()) {
            refreshTokenRevoker.revokeAll(found.getMemberId());
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_REUSED);
        }

        if (!found.isUsable(LocalDateTime.now())) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        found.revoke();
        String newRawToken = issue(found.getMemberId());

        return new RotationResult(found.getMemberId(), newRawToken);
    }

    private String generateRawToken() {
        return UuidCreator.getTimeOrderedEpoch().toString() + UuidCreator.getTimeOrderedEpoch();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    public record RotationResult(UUID memberId, String newRefreshToken) {
    }
}
