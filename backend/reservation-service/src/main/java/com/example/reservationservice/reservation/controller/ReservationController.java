package com.example.reservationservice.reservation.controller;

import com.example.reservationservice.reservation.dto.*;
import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;
import com.example.reservationservice.payment.dto.PaymentResult;
import com.example.reservationservice.qrticket.entity.QrTicket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.reservation.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import javax.swing.plaf.PanelUI;
import javax.swing.text.html.HTML;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final TraceIdProvider traceIdProvider;

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<ReservationResult>> createHold(
            @RequestBody CreateHoldRequest request, HttpServletRequest httpRequest) {
        ReservationResult result = reservationService.createHoldOrQueue(
                request.sessionId(),
                request.memberId(),
                request.headcount(),
                request.attendees(),
                request.groupAttendee()
        );

        String traceId = traceIdProvider.resolve(httpRequest);

        if ("QUEUED".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.success("정원 초과로 대기열에 등록되었습니다", result, traceId));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("예약 처리 완료", result, traceId));
    }

    @GetMapping("/{reservationId}/queue-position")
    public ResponseEntity<ApiResponse<Integer>> getQueuePosition(
            @PathVariable UUID reservationId, HttpServletRequest httpRequest) {
        int position = reservationService.getQueuePosition(reservationId);
        return ResponseEntity.ok(
                ApiResponse.success("순번 조회 완료", position, traceIdProvider.resolve(httpRequest)));
    }

    public record CreateHoldRequest(
            UUID sessionId,
            UUID memberId,
            int headcount,
            List<AttendeeInfo> attendees,
            AttendeeInfo groupAttendee
    ) {}
    @PostMapping("/{reservationId}/payment")
    public ResponseEntity<ApiResponse<PaymentResult>> processPayment(
            @PathVariable UUID reservationId,
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        PaymentResult result = reservationService.processPayment(reservationId, request.paymentId());
        return ResponseEntity.ok(
                ApiResponse.success("결제 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MyReservationResponse>>> getMyReservation(
            @RequestParam UUID memberId,
            HttpServletRequest httpRequest) {
        List<MyReservationResponse> reservations = reservationService.getMyReservations(memberId);
        return ResponseEntity.ok(
                ApiResponse.success("내 예약 목록 조회 완료", reservations, traceIdProvider.resolve(httpRequest)));
    }

    @GetMapping("/sessions/{sessionId}/capacity-status")
    public ResponseEntity<ApiResponse<SessionCapacityStatusResponse>> getCapacityStatus(
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest) {
        SessionCapacityStatusResponse result = reservationService.getCapacityStatus(sessionId);
        return ResponseEntity.ok(
                ApiResponse.success("정원 현황 조회 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    @GetMapping("/sessions/{sessionId}/status-summary")
    public ResponseEntity<ApiResponse<SessionStatusSummaryResponse>> getStatusSummary(
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest) {
        SessionStatusSummaryResponse result = reservationService.getStatusSummary(sessionId);
        return ResponseEntity.ok(
                ApiResponse.success("세션 상태 집계 조회 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<CancelResult>> cancelReservation(
            @PathVariable UUID reservationId,
            HttpServletRequest httpRequest) {
        CancelResult result = reservationService.cancelReservation(reservationId);
        return ResponseEntity.ok(
                ApiResponse.success("예약 취소 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    public record PaymentRequest(String paymentId) {}
}