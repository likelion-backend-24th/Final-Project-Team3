package com.example.reservationservice.payment.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.pgcredential.dto.PgConfigResponse;
import com.example.reservationservice.pgcredential.service.PgCredentialService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 참가자가 결제창(PortOne SDK)을 초기화할 때 쓰는 공개 설정 조회. 로그인만 하면 되고 ADMIN 권한은 불필요.
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentConfigController {

    private static final String PROVIDER = "PORTONE";

    private final PgCredentialService pgCredentialService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping("/pg-config")
    public ResponseEntity<ApiResponse<PgConfigResponse>> getPgConfig(HttpServletRequest httpRequest) {
        PgConfigResponse response = pgCredentialService.getPublicConfig(PROVIDER);
        return ResponseEntity.ok(
                ApiResponse.success("PG 설정 조회 완료", response, traceIdProvider.resolve(httpRequest)));
    }
}
