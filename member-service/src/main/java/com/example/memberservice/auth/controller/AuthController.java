package com.example.memberservice.auth.controller;

import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.auth.dto.LoginResponse;
import com.example.memberservice.auth.dto.RefreshRequest;
import com.example.memberservice.auth.service.AuthService;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TraceIdProvider traceIdProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        LoginResponse response = authService.login(request);
        String traceId = traceIdProvider.resolve(httpRequest);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", response, traceId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest
    ) {
        LoginResponse response = authService.reissue(request.refreshToken());
        String traceId = traceIdProvider.resolve(httpRequest);
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response, traceId));
    }
}
