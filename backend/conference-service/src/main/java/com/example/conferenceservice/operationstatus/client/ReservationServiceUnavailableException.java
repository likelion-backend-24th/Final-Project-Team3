package com.example.conferenceservice.operationstatus.client;

import java.util.UUID;

public class ReservationServiceUnavailableException extends RuntimeException {
    public ReservationServiceUnavailableException(UUID sessionId, Throwable cause) {
        super("Reservation-Service 응답 실패: sessionId=" + sessionId, cause);
    }
}
