package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "참가자 회원가입 요청")
public record SignupRequest(
        @Schema(description = "이메일 (사전에 인증 완료된 주소여야 함)", example = "user@example.com")
        @NotBlank @Email String email,

        @Schema(description = "비밀번호 (8~72자)", example = "password1234")
        @NotBlank @Size(min = 8, max = 72) String password,

        @Schema(description = "이름", example = "홍길동")
        @NotBlank String name,

        @Schema(description = "연령대")
        @NotNull AgeGroup ageGroup,

        @Schema(description = "직무")
        @NotNull Job job
){}
