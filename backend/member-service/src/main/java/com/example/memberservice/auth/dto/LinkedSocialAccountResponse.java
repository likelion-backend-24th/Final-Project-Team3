package com.example.memberservice.auth.dto;

import com.example.memberservice.auth.entity.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "연동된 소셜 계정")
public record LinkedSocialAccountResponse(
        @Schema(description = "Provider") SocialProvider provider,
        @Schema(description = "연동된 시각") LocalDateTime linkedAt
) {}
