package com.example.memberservice.auth.passwordreset.controller;

import com.example.memberservice.auth.passwordreset.dto.PasswordResetConfirmRequest;
import com.example.memberservice.auth.passwordreset.dto.PasswordResetSendCodeRequest;
import com.example.memberservice.auth.passwordreset.service.PasswordResetService;
import com.example.memberservice.common.TraceIdProvider;
import com.example.memberservice.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "비밀번호 재설정 코드 발송", description = "6자리 인증코드를 이메일로 발송한다(유효기간 10분). 존재하지 않는 이메일이거나 소셜 전용 계정이면 거절되고, 재발송 쿨다운(1분) 내 재요청도 거절된다.")
    @PostMapping("/reset-request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetSendCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        passwordResetService.requestReset(request.email());
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok(ApiResponse.success("비밀번호 재설정 코드를 발송했습니다.", traceId));
    }

    @Operation(summary = "비밀번호 재설정 확인", description = "발송된 인증코드를 검증하고 새 비밀번호로 변경한다. 성공 시 해당 계정의 모든 Refresh Token이 무효화된다(전체 기기 재로그인 필요). 5회 실패 시 재발송이 필요하다.")
    @PostMapping("/reset-confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest httpRequest
    ) {
        passwordResetService.confirmReset(request.email(), request.code(), request.newPassword());
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok(ApiResponse.success("비밀번호가 재설정되었습니다.", traceId));
    }
}
