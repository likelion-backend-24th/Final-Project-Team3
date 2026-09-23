package com.example.memberservice.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 계정 연동 요청")
public record SocialLinkRequest(
        @Schema(description = "Google ID Token 또는 Kakao 인가 코드")
        @NotBlank String token,

        @Schema(description = "Kakao Auth.authorize() 호출 시 쓴 redirectUri (Kakao 전용, Google은 무시됨)")
        String redirectUri
) {}
