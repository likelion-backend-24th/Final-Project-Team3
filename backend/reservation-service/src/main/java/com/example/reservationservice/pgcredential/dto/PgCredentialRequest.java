package com.example.reservationservice.pgcredential.dto;

import jakarta.validation.constraints.NotBlank;

public record PgCredentialRequest(
        @NotBlank String provider,
        @NotBlank String storeId,
        @NotBlank String channelKey,
        @NotBlank String apiSecret,
        @NotBlank String webhookSecret
) {}
