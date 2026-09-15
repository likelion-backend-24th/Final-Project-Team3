package com.example.reservationservice.payment.dto;

import java.util.UUID;

public record PaymentResult(UUID reservationId, String status, int qrTicketCount) {
    public static PaymentResult confirmed(UUID reservationId, int qrTicketCount) {
        return new PaymentResult(reservationId, "CONFIRMED", qrTicketCount);
    }
}
