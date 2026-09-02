package org.example.reservationservice.reservation.dto;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ReservationResult {

    private final UUID reservationId;
    private final String status; // "HOLD" or "QUEUED"
    private final Integer queuePosition; // QUEUED일 때만 값 있음

    private ReservationResult(UUID reservationId, String status, Integer queuePosition) {
        this.reservationId = reservationId;
        this.status = status;
        this.queuePosition = queuePosition;
    }

    public static ReservationResult hold(UUID reservationId) {
        return new ReservationResult(reservationId, "HOLD", null);
    }

    public static ReservationResult queued(UUID reservationId, int position) {
        return new ReservationResult(reservationId, "QUEUED", position);
    }
}
