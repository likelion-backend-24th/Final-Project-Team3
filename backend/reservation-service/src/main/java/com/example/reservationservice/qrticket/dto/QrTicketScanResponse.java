package com.example.reservationservice.qrticket.dto;

import com.example.reservationservice.qrticket.entity.QrTicket;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "QR 티켓 스캔(입장 처리) 결과")
public record QrTicketScanResponse(
        @Schema(description = "QR 코드 문자열")
        String code,

        @Schema(description = "사용(입장 처리) 여부", example = "true")
        boolean used,

        @Schema(description = "입장 처리된 시각")
        LocalDateTime usedAt
) {
    public static QrTicketScanResponse from(QrTicket ticket) {
        return new QrTicketScanResponse(ticket.getCode(), ticket.isUsed(), ticket.getUsedAt());
    }
}