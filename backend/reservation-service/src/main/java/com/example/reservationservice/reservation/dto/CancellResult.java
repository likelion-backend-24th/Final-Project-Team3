package com.example.reservationservice.reservation.dto;

import java.util.UUID;

public record CancellResult(
        UUID reservationId,
        String status,
        Integer refundRate,
        Integer refundAmount
) {
    public static CancellResult cancelled(UUID reservationId, Integer refundRate, Integer refundAmount) {
        return new CancellResult(reservationId, "CANCELLED", refundRate, refundAmount);
    }
}