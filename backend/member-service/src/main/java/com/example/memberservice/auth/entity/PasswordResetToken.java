package com.example.memberservice.auth.entity;

import com.example.memberservice.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token", indexes = @Index(name = "idx_password_reset_token_email", columnList = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private int attempts;

    @Builder
    private PasswordResetToken(String email, String codeHash, LocalDateTime expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.used = false;
        this.attempts = 0;
    }

    public static PasswordResetToken issue(String email, String codeHash, LocalDateTime expiresAt) {
        return PasswordResetToken.builder()
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

    public void markUsed() {
        this.used = true;
    }

    public void increaseAttempts() {
        this.attempts++;
    }
}