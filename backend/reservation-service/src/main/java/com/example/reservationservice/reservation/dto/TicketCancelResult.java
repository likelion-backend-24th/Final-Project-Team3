package com.example.reservationservice.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "예약 인원 개별 취소 결과")
public record TicketCancelResult(
        @Schema(description = "취소된 QR 티켓 ID") UUID ticketId,
        @Schema(description = "환불율(%)", example = "100") int refundRate,
        @Schema(description = "환불 금액(원)", example = "5000") int refundAmount,
        @Schema(description = "취소 후 남은 인원 수", example = "2") int remainingHeadcount
) {
}
