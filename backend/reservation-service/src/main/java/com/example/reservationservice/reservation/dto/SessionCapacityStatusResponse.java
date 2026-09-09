package com.example.reservationservice.reservation.dto;

import java.util.UUID;

public record SessionCapacityStatusResponse (
        UUID sessionId,
        int capacity,
        int confirmedCount,
        int remaining
) {}
