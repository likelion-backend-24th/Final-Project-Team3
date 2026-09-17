package com.example.conferenceservice.organizerprofile.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizerConferenceResponse(
        UUID conferenceId,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String location,
        String summaryText
) {}
