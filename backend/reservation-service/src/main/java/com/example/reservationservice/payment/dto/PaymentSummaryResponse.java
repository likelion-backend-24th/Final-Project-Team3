package com.example.reservationservice.payment.dto;

public record PaymentSummaryResponse(
        int totalRevenue,
        int refundedAmount,
        int netRevenue,
        int confirmedCount,
        int cancelledCount
) {}