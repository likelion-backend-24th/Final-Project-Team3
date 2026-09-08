package com.example.memberservice.auth.controller;

import com.example.memberservice.auth.dto.SendCodeRequest;
import com.example.memberservice.auth.service.EmailVerificationService;
import com.example.memberservice.common.TraceIdProvider;
import com.example.memberservice.common.dto.ApiResponse;
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

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(
            @Valid @RequestBody SendCodeRequest request,
            HttpServletRequest httpRequest
    ){
        emailVerificationService.sendCode(request.email());
        String traceId = traceIdProvider.resolve(httpRequest);

        return ResponseEntity.ok(ApiResponse.success("인증코드를 발송했습니다.", traceId));
    }
}
