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

    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "secret_key", nullable = false, columnDefinition = "TEXT")
    private String secretKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public PgCredential(String provider, String apiKey, String secretKey) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.provider = provider;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.updatedAt = LocalDateTime.now();
    }
}
