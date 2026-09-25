package com.example.memberservice.auth.passwordreset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 재설정 코드 발송 요청")
public record PasswordResetRequest(
        @Schema(description = "비밀번호를 재설정할 계정 이메일", example = "user@example.com")
        @NotBlank @Email String email
) {}
