package com.example.reservationservice.payment.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.pgcredential.dto.PgConfigResponse;
import com.example.reservationservice.pgcredential.service.PgCredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 참가자가 결제창(PortOne SDK)을 초기화할 때 쓰는 공개 설정 조회. 로그인만 하면 되고 ADMIN 권한은 불필요.
@Tag(name = "결제", description = "PortOne 웹훅·결제 설정 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentConfigController {

    private static final String PROVIDER = "PORTONE";

    private final PgCredentialService pgCredentialService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "PG 공개 설정 조회", description = "결제창(PortOne SDK) 초기화에 필요한 provider, storeId, channelKey만 반환한다. "
            + "apiSecret·webhookSecret은 응답에 없다. 로그인한 사용자면 호출할 수 있고, 등록된 PG 설정이 없으면 404")
    @GetMapping("/pg-config")
    public ResponseEntity<ApiResponse<PgConfigResponse>> getPgConfig(HttpServletRequest httpRequest) {
        PgConfigResponse response = pgCredentialService.getPublicConfig(PROVIDER);
        return ResponseEntity.ok(
                ApiResponse.success("PG 설정 조회 완료", response, traceIdProvider.resolve(httpRequest)));
    }
}
