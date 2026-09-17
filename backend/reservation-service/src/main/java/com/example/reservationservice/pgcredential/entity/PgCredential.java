package com.example.reservationservice.pgcredential.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pg_credential")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PgCredential {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String provider;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "channel_key", nullable = false)
    private String channelKey;

    @Column(name = "api_secret", nullable = false, columnDefinition = "TEXT")
    private String apiSecret;

    @Column(name = "webhook_secret", nullable = false, columnDefinition = "TEXT")
    private String webhookSecret;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public PgCredential(String provider, String storeId, String channelKey, String apiSecret, String webhookSecret) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.provider = provider;
        this.storeId = storeId;
        this.channelKey = channelKey;
        this.apiSecret = apiSecret;
        this.webhookSecret = webhookSecret;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String storeId, String channelKey, String apiSecret, String webhookSecret) {
        this.storeId = storeId;
        this.channelKey = channelKey;
        this.apiSecret = apiSecret;
        this.webhookSecret = webhookSecret;
        this.updatedAt = LocalDateTime.now();
    }
}
