package com.example.memberservice.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인/토큰 재발급 응답")
public record LoginResponse(
        @Schema(description = "Access Token (Authorization 헤더에 Bearer로 전달)") String accessToken,
        @Schema(description = "토큰 타입", example = "Bearer") String tokenType,
        @Schema(description = "Access Token 만료까지 남은 초") long expiresInSeconds
) {
}
