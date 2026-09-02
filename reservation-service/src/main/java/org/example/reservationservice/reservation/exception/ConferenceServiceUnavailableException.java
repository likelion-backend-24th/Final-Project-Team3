package org.example.reservationservice.reservation.exception;

import java.util.UUID;

public class ConferenceServiceUnavailableException extends RuntimeException {
    public ConferenceServiceUnavailableException(UUID sessionId,Throwable cause) {
        super("Conference-Service 응답 실패: sessionId=" + sessionId, cause);
    }
}
