package com.example.memberservice.member.service;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 탈퇴 요청 — 비밀번호 계정은 password만, 소셜 전용 계정은 provider·socialToken(카카오는 redirectUri도)만 채운다")
public record WithdrawRequest(
        @Schema(description = "비밀번호 계정 본인확인용 현재 비밀번호") String password,
        @Schema(description = "소셜 전용 계정 재인증용 provider (google/kakao)") String provider,
        @Schema(description = "소셜 전용 계정 재인증용 토큰") String socialToken,
        @Schema(description = "카카오 재인증 시 필요한 redirectUri") String redirectUri
) {}
