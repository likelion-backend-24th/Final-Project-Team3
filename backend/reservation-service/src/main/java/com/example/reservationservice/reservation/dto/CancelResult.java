package com.example.reservationservice.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "예약 취소 결과")
public record CancelResult(
        @Schema(description = "취소된 예약 ID")
        UUID reservationId,

        @Schema(description = "취소 후 상태", example = "CANCELLED")
        String status,

        @Schema(description = "환불율(%). HOLD/QUEUED 취소 시에는 null", example = "100", nullable = true)
        Integer refundRate,

        @Schema(description = "환불 금액. HOLD/QUEUED 취소 시에는 null", example = "10000", nullable = true)
        Integer refundAmount
) {
    public static CancelResult cancelled(UUID reservationId, Integer refundRate, Integer refundAmount) {
        return new CancelResult(reservationId, "CANCELLED", refundRate, refundAmount);
    }
}