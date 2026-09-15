package com.example.reservationservice.pgcredential.dto;

import jakarta.validation.constraints.NotBlank;

public record PgCredentialRequest(
        @NotBlank String provider,
        @NotBlank String apiKey,
        @NotBlank String secretKey
) {}
