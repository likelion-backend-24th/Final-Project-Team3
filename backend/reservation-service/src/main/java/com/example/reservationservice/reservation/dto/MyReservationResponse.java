package com.example.reservationservice.reservation.dto;

import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MyReservationResponse(
        UUID reservationId,
        UUID sessionId,
        ReservationStatus status,
        int headcount,
        LocalDateTime createdAt
) {
    public static MyReservationResponse from(Reservation reservation) {
        return new MyReservationResponse(
                reservation.getId(),
                reservation.getSessionId(),
                reservation.getStatus(),
                reservation.getHeadcount(),
                reservation.getCreatedAt()
        );
    }
}
