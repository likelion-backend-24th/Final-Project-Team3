package com.example.memberservice.auth.session.service;

import com.example.memberservice.auth.session.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 비밀번호 재설정·회원 탈퇴 시 회원의 모든 Refresh Token을 무효화하는 전용 클래스.
 * 별도 빈 + REQUIRES_NEW로 독립된 트랜잭션에서 즉시 커밋된다.
 * 주의: 호출하는 쪽 트랜잭션이 이미 refresh_token 행을 잠근 상태라면 그 잠금을 기다리다 Lock wait timeout이 난다.
 * Refresh Token 재사용 탐지(RefreshTokenService.rotate())가 그 경우라서 거기서는 이 클래스를 쓰지 않고 같은 트랜잭션에서 폐기한다.
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
