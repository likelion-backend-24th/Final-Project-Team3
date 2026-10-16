package com.example.memberservice.auth.session.service;

import com.example.memberservice.auth.session.entity.RefreshToken;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.exception.RefreshTokenReusedException;
import com.example.memberservice.auth.session.repository.RefreshTokenRepository;
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

    // 재사용을 감지하면 전체 폐기 후 예외를 던지는데, 그 폐기는 커밋되어야 하므로 이 예외로는 롤백하지 않는다.
    // 호출하는 AuthService.reissue()도 같은 트랜잭션이라 같은 설정이 필요하다.
    @Transactional(noRollbackFor = RefreshTokenReusedException.class)
    public RotationResult rotate(String rawToken) {
        RefreshToken found = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (found.isExpired(LocalDateTime.now())) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        int revokedRows = refreshTokenRepository.revokeIfActive(found.getId());
        if (revokedRows == 0) {
            // 이미 폐기된 토큰이 다시 제출됨 -> 재사용(탈취) 의심, 동시 레이스에서 진 경우도 여기로 들어옴
            // 전체 폐기는 같은 트랜잭션에서 한다. 위 조건부 UPDATE가 0행이어도 MySQL(REPEATABLE READ)은 이 행의 잠금을 유지하므로,
            // 별도 트랜잭션(REQUIRES_NEW)으로 폐기하면 그 잠금을 기다리다 Lock wait timeout(1205)이 난다.
            refreshTokenRepository.revokeAllByMemberId(found.getMemberId());
            throw new RefreshTokenReusedException();
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
