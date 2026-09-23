package com.example.memberservice.auth.passwordreset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 재설정 확인 요청")
public record PasswordResetConfirm(
        @Schema(description = "재설정 대상 이메일", example = "user@example.com")
        @NotBlank @Email String email,

        @Schema(description = "발송받은 6자리 인증코드", example = "123456")
        @NotBlank @Pattern(regexp = "\\d{6}") String code,

        @Schema(description = "새 비밀번호 (8~72자)", example = "newPassword1234")
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {}
