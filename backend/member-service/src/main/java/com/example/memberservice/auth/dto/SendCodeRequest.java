package com.example.memberservice.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일 인증코드 발송 요청")
public record SendCodeRequest(
   @Schema(description = "인증코드를 받을 이메일 (이미 가입된 이메일이면 거절됨)", example = "user@example.com")
   @NotBlank @Email String email
) {}
