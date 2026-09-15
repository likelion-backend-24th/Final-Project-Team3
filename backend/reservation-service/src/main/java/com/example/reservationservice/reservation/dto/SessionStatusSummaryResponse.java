package com.example.reservationservice.reservation.dto;

import java.util.UUID;

public record SessionStatusSummaryResponse(
        UUID sessionId,
        long holdCount,
        long queuedCount,
        long confirmedCount,
        long cancelledCount,
        long checkedInCount
) {}