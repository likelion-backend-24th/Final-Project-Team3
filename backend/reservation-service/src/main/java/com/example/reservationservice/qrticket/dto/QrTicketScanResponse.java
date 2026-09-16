package com.example.reservationservice.qrticket.dto;

import com.example.reservationservice.qrticket.entity.QrTicket;

import java.time.LocalDateTime;

public record QrTicketScanResponse(
        String code,
        boolean used,
        LocalDateTime usedAt
) {
    public static QrTicketScanResponse from(QrTicket ticket) {
        return new QrTicketScanResponse(ticket.getCode(), ticket.isUsed(), ticket.getUsedAt());
    }
}