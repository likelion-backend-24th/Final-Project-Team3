package com.example.memberservice.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "참가자 회원가입 응답")
public record SignupResponse(
        @Schema(description = "생성된 회원 ID") UUID memberId,
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "이름", example = "홍길동") String name,
        @Schema(description = "권한", example = "MEMBER") String role
) {}
