package com.example.memberservice.auth.passwordreset.service;

import com.example.memberservice.auth.passwordreset.entity.PasswordResetToken;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.passwordreset.repository.PasswordResetTokenRepository;
import com.example.memberservice.auth.service.EmailSender;
import com.example.memberservice.auth.session.service.RefreshTokenRevoker;
import com.example.memberservice.common.exception.BusinessException;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.exception.MemberErrorCode;
import com.example.memberservice.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import static com.example.memberservice.common.HashUtil.sha256;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MemberRepository memberRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRevoker refreshTokenRevoker;
    private final PasswordResetAttemptRecorder attemptRecorder;

    @Value("${password.reset-code-validity-ms}")
    private long validityMs;

    @Value("${password.max-attempts}")
    private int maxAttempts;

    @Value("${password.resend-cooldown-ms}")
    private long resendCooldownMs;

    @Transactional
    public void requestReset(String rawEmail) {
        String email = normalize(rawEmail);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.getPassword() == null) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_SOCIAL_ONLY_ACCOUNT);
        }

        passwordResetTokenRepository.findFirstByEmailOrderByCreatedAtDesc(email)
                .filter(t -> t.isResendTooSoon(LocalDateTime.now(), resendCooldownMs))
                .ifPresent(t -> { throw new BusinessException(AuthErrorCode.PASSWORD_RESET_RESEND_TOO_SOON); });

        passwordResetTokenRepository.deleteAllByEmail(email);

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(validityMs * 1_000_000);
        passwordResetTokenRepository.save(PasswordResetToken.issue(email, sha256(code), expiresAt));

        emailSender.sendPasswordResetCode(email, code, validityMs / 60_000);
    }

    @Transactional
    public void confirmReset(String rawEmail, String code, String newPassword) {
        String email = normalize(rawEmail);

        PasswordResetToken token = passwordResetTokenRepository
                .findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.PASSWORD_RESET_CODE_INVALID));

        if (token.isExpired(LocalDateTime.now())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_CODE_EXPIRED);
        }

        if (token.getAttempts() >= maxAttempts) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_ATTEMPTS_EXCEEDED);
        }

        if (!token.getCodeHash().equals(sha256(code))) {
            attemptRecorder.increaseAttempts(token.getId());
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_CODE_INVALID);
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        token.markUsed();
        member.changePassword(passwordEncoder.encode(newPassword));
        refreshTokenRevoker.revokeAll(member.getId());
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String normalize(String email) {
        return email.strip().toLowerCase();
    }
}