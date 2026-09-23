package com.example.memberservice.auth.emailverification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "이메일 인증코드 검증 요청")
public record VerifyCodeRequest(
   @Schema(description = "인증 대상 이메일", example = "user@example.com")
   @NotBlank @Email String email,

   @Schema(description = "발송받은 6자리 인증코드", example = "123456")
   @NotBlank @Pattern(regexp = "\\d{6}") String code
) {}
