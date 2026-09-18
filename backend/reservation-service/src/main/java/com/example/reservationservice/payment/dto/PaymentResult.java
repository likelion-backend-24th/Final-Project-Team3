package com.example.reservationservice.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "결제 처리 결과")
public record PaymentResult(

        @Schema(description = "결제 완료된 에약 ID")
        UUID reservationId,

        @Schema(description = "결제 상태", example = "CONFIRMED")
        String status,

        @Schema(description = "발급된 QR 티켓 개수", example = "2")
        int qrTicketCount) {
    public static PaymentResult confirmed(UUID reservationId, int qrTicketCount) {
        return new PaymentResult(reservationId, "CONFIRMED", qrTicketCount);
    }
}
