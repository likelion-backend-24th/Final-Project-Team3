package com.example.reservationservice.pgcredential.dto;

import com.example.reservationservice.pgcredential.entity.PgCredential;

import java.time.LocalDateTime;

public record PgCredentialResponse(
        String provider,
        String apiKey,
        String secretKey,
        LocalDateTime updatedAt
) {
    public static PgCredentialResponse of(PgCredential credential, String maskedSecretKey) {
        return new PgCredentialResponse(
                credential.getProvider(),
                credential.getApiKey(),
                maskedSecretKey,
                credential.getUpdatedAt()
        );
    }
}
