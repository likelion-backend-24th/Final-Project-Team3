package com.example.memberservice.auth.dto;

import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 로그인 요청")
public record SocialLoginRequest(
        @Schema(description = "Google ID Token 또는 Kakao Access Token")
        @NotBlank String token,

        @Schema(description = "연령대 (최초 가입 시 필수, 재로그인 시 무시됨)")
        AgeGroup ageGroup,

        @Schema(description = "직무 (최초 가입 시 필수, 재로그인 시 무시됨)")
        Job job
) {}
