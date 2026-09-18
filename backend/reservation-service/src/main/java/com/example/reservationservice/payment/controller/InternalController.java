package com.example.reservationservice.payment.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.payment.dto.PaymentSummaryResponse;
import com.example.reservationservice.payment.service.PaymentService;
import com.example.reservationservice.reservation.dto.AttendeeCheckinStatsResponse;
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

/**
 * 서비스 간(Conference-Service → Reservation-Service) 내부 통신 전용 API.
 * Gateway가 /internal/** 경로를 외부에 노출하지 않으므로, 외부 클라이언트는 호출할 수 없다.
 * Swagger 문서에는 노출되지 않는다(springdoc.paths-to-exclude 설정).
 */
@RestController
@RequestMapping("/internal/sessions")
@RequiredArgsConstructor
public class InternalController {

    private final PaymentService paymentService;
    private final ReservationService reservationService;
    private final TraceIdProvider traceIdProvider;

    /**
     * 세션별 결제 정산 집계 조회.
     * Conference-Service가 주최자 정산 내역 조회(Story 17) 시 호출한다.
     *
     * @param sessionIds 집계할 세션 ID 목록 (컨퍼런스 소속 세션 전체)
     * @return 매출·환불·순매출·건수 집계 결과
     */
    @GetMapping("/payment-summary")
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getPaymentSummary(
            @RequestParam List<UUID> sessionIds,
            HttpServletRequest httpRequest) {
        PaymentSummaryResponse result = paymentService.getPaymentSummary(sessionIds);
        return ResponseEntity.ok(
                ApiResponse.success("정산 집계 조회 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    /**
     * 세션별 체크인 참가자 연령대·직무 분포 집계 조회.
     * Conference-Service가 참석자 통계(Story 15) 조회 시 호출한다.
     *
     * @param sessionIds 집계할 세션 ID 목록
     * @return 체크인 완료 인원 수 및 연령대·직무별 분포
     */
    @GetMapping("/attendee-checkin-stats")
    public ResponseEntity<ApiResponse<AttendeeCheckinStatsResponse>> getAttendeeCheckinStats(
            @RequestParam List<UUID> sessionIds,
            HttpServletRequest httpRequest) {
        AttendeeCheckinStatsResponse result = reservationService.getAttendeeCheckinStats(sessionIds);
        return ResponseEntity.ok(
                ApiResponse.success("체크인 집계 조회 완료", result, traceIdProvider.resolve(httpRequest)));
    }
}