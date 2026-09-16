package com.example.conferenceservice.settlement.client;

import java.util.List;
import java.util.UUID;

public class PaymentSummaryUnavailableException extends RuntimeException {
    public PaymentSummaryUnavailableException(List<UUID> sessionIds, Throwable cause) {
        super("Reservation-Service 응답 실패: sessionIds=" + sessionIds, cause);
    }
}
