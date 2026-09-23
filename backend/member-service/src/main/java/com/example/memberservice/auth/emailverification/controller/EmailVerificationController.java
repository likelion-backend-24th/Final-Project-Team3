package com.example.memberservice.auth.emailverification.controller;

import com.example.memberservice.auth.emailverification.dto.SendCodeRequest;
import com.example.memberservice.auth.emailverification.dto.VerifyCodeRequest;
import com.example.memberservice.auth.emailverification.service.EmailVerificationService;
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
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "이메일 인증코드 발송", description = "6자리 인증코드를 이메일로 발송한다(유효기간 10분). 이미 가입된 이메일이면 거절되고, 재발송 쿨다운(1분) 내 재요청도 거절된다.")
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(
            @Valid @RequestBody SendCodeRequest request,
            HttpServletRequest httpRequest
    ){
        emailVerificationService.sendCode(request.email());
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok(ApiResponse.success("인증코드를 발송했습니다.", traceId));
    }

    @Operation(summary = "이메일 인증코드 검증", description = "발송된 인증코드를 검증해 이메일을 인증완료 상태로 마킹한다(회원가입 전 단계). 5회 실패 시 재발송이 필요하다.")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
            @Valid @RequestBody VerifyCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        emailVerificationService.verifyCode(request.email(), request.code());
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다.", traceId));
    }
}
