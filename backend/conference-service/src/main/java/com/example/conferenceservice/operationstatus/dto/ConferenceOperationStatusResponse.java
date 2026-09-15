package com.example.conferenceservice.operationstatus.dto;

import java.util.List;
import java.util.UUID;

public record ConferenceOperationStatusResponse(
        UUID conferenceId,
        List<SessionOperationStatus> sessions
) {
    public record SessionOperationStatus(
            UUID sessionId,
            String title,
            long holdCount,
            long queuedCount,
            long confirmedCount,
            long cancelledCount,
            long checkedInCount
    ) {}
}
