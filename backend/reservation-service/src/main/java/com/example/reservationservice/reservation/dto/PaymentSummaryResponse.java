package com.example.reservationservice.reservation.dto;

public record PaymentSummaryResponse(
        int totalRevenue,
        int refundedAmount,
        int netRevenue,
        int confirmedCount,
        int cancelledCount
) {}