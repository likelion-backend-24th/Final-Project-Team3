package com.example.memberservice.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 계정 연동 요청")
public record SocialLinkRequest(
        @Schema(description = "Google ID Token 또는 Kakao Access Token")
        @NotBlank String token
) {}
