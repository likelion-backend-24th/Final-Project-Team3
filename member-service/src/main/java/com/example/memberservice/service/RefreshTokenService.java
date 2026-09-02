package com.example.memberservice.service;

import com.example.memberservice.entity.RefreshToken;
import com.example.memberservice.repository.RefreshTokenRepository;
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
                .orElseThrow(() -> new InvalidRefreshTokenException("존재하지 않는 토큰입니다."));

        if (found.isRevoked()) {
            refreshTokenRepository.revokeAllByMemberId(found.getMemberId());
            throw new InvalidRefreshTokenException("이미 사용된 토큰입니다. 재로그인이 필요합니다.");
        }

        if (!found.isUsable(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("만료된 토큰입니다.");
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
