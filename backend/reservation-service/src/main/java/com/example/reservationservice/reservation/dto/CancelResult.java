package com.example.reservationservice.reservation.dto;

import java.util.UUID;

public record CancelResult(
        UUID reservationId,
        String status,
        Integer refundRate,
        Integer refundAmount
) {
    public static CancelResult canceled(UUID reservationId, Integer refundRate, Integer refundAmount) {
        return new CancelResult(reservationId, "CANCELLED", refundRate, refundAmount);
    }
}