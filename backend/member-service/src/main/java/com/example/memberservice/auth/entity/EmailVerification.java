package com.example.memberservice.auth.entity;

import com.example.memberservice.common.BaseEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="email_verification", indexes = @Index(name = "idx_email_verification_email", columnList = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name="code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name="expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private int attempts;

    @Builder
    private EmailVerification(String email, String codeHash, LocalDateTime expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.attempts = 0;
    }

    public static EmailVerification issue(String email, String codeHash, LocalDateTime expiresAt) {
        return EmailVerification.builder()
                .email(email)
                .codeHash(codeHash)
                .expiresAt(expiresAt)
                .build();
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public boolean isResendTooSoon(LocalDateTime now, long cooldownMs) {
        return getCreatedAt().plusNanos(cooldownMs * 1_000_000).isAfter(now);
    }

    public void markVerified() {
        this.verified = true;
    }

    public void increaseAttempts() {
        this.attempts++;
    }

}
