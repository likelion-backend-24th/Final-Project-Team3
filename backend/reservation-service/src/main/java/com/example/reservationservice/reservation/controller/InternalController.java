package com.example.reservationservice.reservation.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.reservation.dto.AttendeeCheckinStatsResponse;
import com.example.reservationservice.reservation.dto.PaymentSummaryResponse;
import com.example.reservationservice.reservation.service.ReservationService;
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

    private final ReservationService reservationService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping("/payment-summary")
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getPaymentSummary(
            @RequestParam List<UUID> sessionIds,
            HttpServletRequest httpRequest) {
        PaymentSummaryResponse result = reservationService.getPaymentSummary(sessionIds);
        return ResponseEntity.ok(
                ApiResponse.success("정산 집계 조회 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    @GetMapping("/attendee-checkin-stats")
    public ResponseEntity<ApiResponse<AttendeeCheckinStatsResponse>> getAttendeeCheckinStats(
            @RequestParam List<UUID> sessionIds,
            HttpServletRequest httpRequest) {
        AttendeeCheckinStatsResponse result = reservationService.getAttendeeCheckinStats(sessionIds);
        return ResponseEntity.ok(
                ApiResponse.success("체크인 집계 조회 완료", result, traceIdProvider.resolve(httpRequest)));
    }
}