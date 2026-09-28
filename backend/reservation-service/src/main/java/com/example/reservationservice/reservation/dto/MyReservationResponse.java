package com.example.reservationservice.reservation.dto;

import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "내 예약 목록 조회 응답")
public record MyReservationResponse(
        @Schema(description = "예약 ID")
        UUID reservationId,

        @Schema(description = "세션 ID")
        UUID sessionId,

        @Schema(description = "예약 상태", example = "CONFIRMED")
        ReservationStatus status,

        @Schema(description = "신청 인원 수", example = "2")
        int headcount,

        @Schema(description = "신청 생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "실제 결제 금액. 결제한 적 없으면(결제 대기·대기열·결제 없이 취소) null", example = "60000", nullable = true)
        Integer paidAmount,

        @Schema(description = "환불된 금액 누계. 결제한 적 없으면 null", example = "30000", nullable = true)
        Integer refundedAmount
) {
    public static MyReservationResponse from(Reservation reservation, Payment payment) {
        return new MyReservationResponse(
                reservation.getId(),
                reservation.getSessionId(),
                reservation.getStatus(),
                reservation.getHeadcount(),
                reservation.getCreatedAt(),
                payment == null ? null : payment.getAmount(),
                payment == null ? null : (payment.getRefundedAmount() == null ? 0 : payment.getRefundedAmount())
        );
    }
}
