package com.example.memberservice.auth.service;

import com.example.memberservice.auth.entity.EmailVerification;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.repository.EmailVerificationRepository;
import com.example.memberservice.common.exception.BusinessException;
import com.example.memberservice.member.exception.MemberErrorCode;
import com.example.memberservice.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository emailVerificationRepository;
    private final MemberRepository memberRepository;
    private final EmailSender emailSender;
    private final EmailVerificationAttemptRecorder attemptRecorder;

    @Value("${email.verification-code-validity-ms}")
    private long validityMs;

    @Value("${email.max-attempts}")
    private int maxAttempts;

    @Transactional
    public void sendCode(String rawEmail){
        String email = normalize(rawEmail);

        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
        }

        // 재발송 시 이전 코드(인증완료 여부 무관)는 전부 무효화하고 새로 발급
        emailVerificationRepository.deleteAllByEmail(email);

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(validityMs * 1_000_000);
        emailVerificationRepository.save(EmailVerification.issue(email, sha256(code), expiresAt));

        emailSender.sendVerificationCode(email, code, validityMs / 60_000);
    }

    @Transactional
    public void verifyCode(String rawEmail, String code) {
        String email = normalize(rawEmail);

        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndVerifiedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(()->new BusinessException(AuthErrorCode.EMAIL_CODE_INVALID));

        if(verification.isExpired(LocalDateTime.now())) {
            throw new BusinessException(AuthErrorCode.EMAIL_CODE_EXPIRED);
        }

        if(verification.getAttempts() >= maxAttempts) {
            throw new BusinessException(AuthErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED);
        }

        if(!verification.getCodeHash().equals(sha256(code))) {
            attemptRecorder.increaseAttempts(verification.getId());
            throw new BusinessException(AuthErrorCode.EMAIL_CODE_INVALID);
        }

        verification.markVerified();    // 관리 상태 엔티티라 dirty checking으로 자동 반영, save() 불필요
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String normalize(String email) {
        return email.strip().toLowerCase();
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

}
