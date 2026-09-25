package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "내 프로필 응답")
public record MemberProfileResponse(
        @Schema(description = "회원 ID") UUID memberId,
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "이름", example = "홍길동") String name,
        @Schema(description = "연령대") AgeGroup ageGroup,
        @Schema(description = "직무") Job job,
        @Schema(description = "비밀번호 보유 여부 — false면 소셜 로그인 전용 계정") boolean hasPassword,
        @Schema(description = "기관명 (주최자만)") String organizationName,
        @Schema(description = "사업자등록번호 (주최자만)") String businessNo
) {}
