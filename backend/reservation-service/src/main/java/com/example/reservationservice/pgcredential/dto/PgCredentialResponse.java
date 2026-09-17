package com.example.reservationservice.pgcredential.dto;

import com.example.reservationservice.pgcredential.entity.PgCredential;

import java.time.LocalDateTime;

public record PgCredentialResponse(
        String provider,
        String storeId,
        String channelKey,
        String apiSecret,
        String webhookSecret,
        LocalDateTime updatedAt
) {
    public static PgCredentialResponse of(PgCredential credential, String maskedApiSecret, String maskedWebhookSecret) {
        return new PgCredentialResponse(
                credential.getProvider(),
                credential.getStoreId(),
                credential.getChannelKey(),
                maskedApiSecret,
                maskedWebhookSecret,
                credential.getUpdatedAt()
        );
    }
}
