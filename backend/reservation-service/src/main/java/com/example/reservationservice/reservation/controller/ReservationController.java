package com.example.reservationservice.reservation.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.reservation.dto.*;
import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;
import com.example.reservationservice.payment.dto.PaymentResult;
import com.example.reservationservice.qrticket.entity.QrTicket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.reservation.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.UUID;

@Tag(name = "예약", description = "세션 신청·결제·취소·조회 API")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "세션 신청", description = "세션에 신청하여 정원 내면 HOLD(10분 결제 대기), 정원 초과면 QUEUED(대기열)로 등록한다")
    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<ReservationResult>> createHold(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateHoldRequest request, HttpServletRequest httpRequest) {
        ReservationResult result = reservationService.createHoldOrQueue(
                request.sessionId(),
                userDetails.getMemberId(),
                request.headcount(),
                request.attendees(),
                request.groupAttendee()
        );

        String traceId = traceIdProvider.resolve(httpRequest);

        if ("QUEUED".equals(result.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.success("정원 초과로 대기열에 등록되었습니다", result, traceId));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("예약 처리 완료", result, traceId));
    }

    @Operation(summary = "대기열 순번 조회", description = "본인 예약의 현재 대기열 순번을 조회한다")
    @GetMapping("/{reservationId}/queue-position")
    public ResponseEntity<ApiResponse<Integer>> getQueuePosition(
            @PathVariable UUID reservationId, HttpServletRequest httpRequest) {
        int position = reservationService.getQueuePosition(reservationId);
        return ResponseEntity.ok(
                ApiResponse.success("순번 조회 완료", position, traceIdProvider.resolve(httpRequest)));
    }

    public record CreateHoldRequest(
            @Schema(description = "신청할 세션 ID")
            UUID sessionId,

            @Schema(description = "신청 인원 수 (1~9명은 attendees, 10명 이상은 groupAttendee 사용)", example = "2")
            int headcount,

            @Schema(description = "9명 이하일 때 개별 입력하는 동반자 정보 목록")
            List<AttendeeInfo> attendees,

            @Schema(description = "10명 이상일 때 일괄 입력하는 동반자 정보(전원 동일하게 적용)")
            AttendeeInfo groupAttendee
    ) {}

    @Operation(summary = "결제 처리", description = "HOLD 또는 대기열 순번 도달 예약에 대해 결제를 진행하고, 성공 시 좌석을 확정하고 QR 티켓을 발급한다")
    @PostMapping("/{reservationId}/payment")
    public ResponseEntity<ApiResponse<PaymentResult>> processPayment(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        PaymentResult result = reservationService.processPayment(
                reservationId,userDetails.getMemberId(), request.paymentMethod(), request.amount());
        return ResponseEntity.ok(
                ApiResponse.success("결제 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    @Operation(summary = "내 예약 목록 조회", description = "로그인한 사용자 본인의 예약 목록을 최신순으로 조회한다")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MyReservationResponse>>> getMyReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {
        List<MyReservationResponse> reservations = reservationService.getMyReservations(userDetails.getMemberId());
        return ResponseEntity.ok(
                ApiResponse.success("내 예약 목록 조회 완료", reservations, traceIdProvider.resolve(httpRequest)));
    }

    @Operation(summary = "세션 정원 현황 조회", description = "세션의 전체 정원, 확정 인원, 잔여 좌석 수를 조회한다")
    @GetMapping("/sessions/{sessionId}/capacity-status")
    public ResponseEntity<ApiResponse<SessionCapacityStatusResponse>> getCapacityStatus(
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest) {
        SessionCapacityStatusResponse result = reservationService.getCapacityStatus(sessionId);
        return ResponseEntity.ok(
                ApiResponse.success("정원 현황 조회 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    @Operation(summary = "세션 신청 상태 집계 조회", description = "세션의 상태별(HOLD/QUEUED/CONFIRMED/CANCELLED) 건수와 체크인 완료 건수를 조회한다")
    @GetMapping("/sessions/{sessionId}/status-summary")
    public ResponseEntity<ApiResponse<SessionStatusSummaryResponse>> getStatusSummary(
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest) {
        SessionStatusSummaryResponse result = reservationService.getStatusSummary(sessionId);
        return ResponseEntity.ok(
                ApiResponse.success("세션 상태 집계 조회 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    @Operation(summary = "예약 취소", description = "결제완료 건은 세션 시작일 기준 환불율을 계산하여 취소하고, HOLD·대기열 건은 환불 없이 즉시 취소한다")
    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<CancelResult>> cancelReservation(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {
        CancelResult result = reservationService.cancelReservation(reservationId, userDetails.getMemberId());
        return ResponseEntity.ok(
                ApiResponse.success("예약 취소 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    @Schema(description = "결제 요청 정보")
    public record PaymentRequest(
            @Schema(description = "결제 수단", example = "CARD")
            String paymentMethod,

            @Schema(description = "결제 금액", example = "20000")
            int amount
    ) {}
}