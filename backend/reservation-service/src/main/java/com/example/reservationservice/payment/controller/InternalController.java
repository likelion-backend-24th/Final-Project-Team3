package com.example.reservationservice.payment.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.payment.dto.PaymentSummaryResponse;
import com.example.reservationservice.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/sessions")
@RequiredArgsConstructor
public class InternalController {

    private final PaymentService paymentService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping("/payment-summary")
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getPaymentSummary(
            @RequestParam List<UUID> sessionIds,
            HttpServletRequest httpRequest) {
        PaymentSummaryResponse result = paymentService.getPaymentSummary(sessionIds);
        return ResponseEntity.ok(
                ApiResponse.success("정산 집계 조회 완료", result, traceIdProvider.resolve(httpRequest)));
    }
}