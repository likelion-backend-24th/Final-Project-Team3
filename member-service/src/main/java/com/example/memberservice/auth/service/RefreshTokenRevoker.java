package com.example.memberservice.auth.service;

import com.example.memberservice.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 재사용 탐지 시 회원의 모든 Refresh Token을 무효화하는 전용 클래스.
 * RefreshTokenService.rotate()는 무효화 직후 예외를 던져 자신의 트랜잭션을 롤백시키므로,
 * 이 무효화만은 별도 빈 + REQUIRES_NEW로 독립된 트랜잭션에서 즉시 커밋되게 한다.
 * (같은 클래스 안에서 self-invocation으로 호출하면 프록시가 적용되지 않아 REQUIRES_NEW가 무시된다.)
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenRevoker {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAll(UUID memberId) {
        refreshTokenRepository.revokeAllByMemberId(memberId);
    }
}
