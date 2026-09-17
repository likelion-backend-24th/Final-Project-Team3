package com.example.conferenceservice.attendeesummary.client;

import java.util.List;
import java.util.UUID;

public class AttendeeStatsUnavailableException extends RuntimeException {
    public AttendeeStatsUnavailableException(List<UUID> sessionIds, Throwable cause) {
        super("Reservation-Service 응답 실패: sessionIds=" + sessionIds, cause);
    }
}
