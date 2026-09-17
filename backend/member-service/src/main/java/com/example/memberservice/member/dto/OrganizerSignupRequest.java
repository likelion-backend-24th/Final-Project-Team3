package com.example.memberservice.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "주최자 회원가입 요청")
public record OrganizerSignupRequest(
        @Schema(description = "이메일 (사전에 인증 완료된 주소여야 함)", example = "organizer@example.com")
        @NotBlank @Email String email,

        @Schema(description = "비밀번호 (8~72자)", example = "password1234")
        @NotBlank @Size(min = 8, max = 72) String password,

        @Schema(description = "이름", example = "홍길동")
        @NotBlank String name,

        @Schema(description = "주최 기관명", example = "멋쟁이사자처럼")
        @NotBlank @Size(max = 100) String organizationName,

        @Schema(description = "사업자등록번호 (10자리)", example = "1234567890")
        @NotBlank String businessNo
) {}
