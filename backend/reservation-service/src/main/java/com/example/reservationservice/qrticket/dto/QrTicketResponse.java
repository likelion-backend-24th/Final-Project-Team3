package com.example.reservationservice.qrticket.dto;

import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "QR 티켓 조회 응답")
public record QrTicketResponse(
        @Schema(description = "QR 티켓 ID")
        UUID id,

        @Schema(description = "QR 코드 문자열")
        String code,

        @Schema(description = "사용(입장 처리) 여부")
        boolean used,

        @Schema(description = "입장 처리된 시각")
        LocalDateTime usedAt,

        @Schema(description = "연령대")
        AgeGroup ageGroup,

        @Schema(description = "직무")
        Job job
) {
    public static QrTicketResponse from(QrTicket ticket) {
        return new QrTicketResponse(
                ticket.getId(),
                ticket.getCode(),
                ticket.isUsed(),
                ticket.getUsedAt(),
                ticket.getAgeGroup(),
                ticket.getJob()
        );
    }
}