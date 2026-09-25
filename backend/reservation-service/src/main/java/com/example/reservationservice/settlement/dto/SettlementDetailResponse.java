package com.example.reservationservice.settlement.dto;

import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.reservation.entity.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "정산 내역 상세 응답(결제 건 1개)")
public record SettlementDetailResponse(
        @Schema(description = "예약 ID") UUID reservationId,
        @Schema(description = "세션 ID. 세션·컨퍼런스 이름은 공개 컨퍼런스 목록 API로 프론트에서 매핑한다") UUID sessionId,
        @Schema(description = "결제한 회원 ID") UUID memberId,
        @Schema(description = "결제 금액(원)", example = "20000") int amount,
        @Schema(description = "결제 완료 시각") LocalDateTime paidAt,
        @Schema(description = "예약 상태: CONFIRMED(확정) / CANCELLED(취소)", example = "CONFIRMED") String reservationStatus,
        @Schema(description = "환불 금액(원). 환불이 없으면 null", nullable = true) Integer refundedAmount,
        @Schema(description = "환불 처리 시각. 환불이 없으면 null", nullable = true) LocalDateTime refundedAt,
        @Schema(description = "발급된 QR 티켓 수(headcount만큼 발급됨)", example = "2") int ticketCount,
        @Schema(description = "체크인(QR 스캔) 완료된 티켓 수", example = "1") int checkedInCount
) {
    public static SettlementDetailResponse of(Payment payment, Reservation reservation, int ticketCount, int checkedInCount) {
        return new SettlementDetailResponse(
                reservation.getId(),
                reservation.getSessionId(),
                reservation.getMemberId(),
                payment.getAmount(),
                payment.getPaidAt(),
                reservation.getStatus().name(),
                payment.getRefundedAmount(),
                payment.getRefundedAt(),
                ticketCount,
                checkedInCount
        );
    }
}
