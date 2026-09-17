package com.example.memberservice.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "주최자 회원가입 응답")
public record OrganizerSignupResponse(
        @Schema(description = "생성된 회원 ID") UUID memberId,
        @Schema(description = "이메일", example = "organizer@example.com") String email,
        @Schema(description = "이름", example = "홍길동") String name,
        @Schema(description = "주최 기관명", example = "멋쟁이사자처럼") String organizationName,
        @Schema(description = "사업자등록번호", example = "1234567890") String businessNo,
        @Schema(description = "권한", example = "ORGANIZER") String role
) {}
