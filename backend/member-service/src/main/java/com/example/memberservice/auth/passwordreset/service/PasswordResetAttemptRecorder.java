package com.example.memberservice.auth.passwordreset.service;

import com.example.memberservice.auth.passwordreset.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 인증 실패 시 시도 횟수를 별도 트랜잭션에서 즉시 커밋
// PasswordResetService.confirmReset()은 실패 시 예외를 던져 자신의 트랜잭션을 롤백시키므로,
// 같은 클래스 안에서 처리하면 시도 횟수 증가도 함께 롤백된다.
@Service
@RequiredArgsConstructor
public class PasswordResetAttemptRecorder {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseAttempts(UUID id) {
        passwordResetTokenRepository.increaseAttempts(id);
    }
}
