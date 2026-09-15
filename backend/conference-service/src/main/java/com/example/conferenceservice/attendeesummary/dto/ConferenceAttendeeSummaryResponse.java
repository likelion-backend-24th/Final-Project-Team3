package com.example.conferenceservice.attendeesummary.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ConferenceAttendeeSummaryResponse(
        UUID conferenceId,
        int checkedInCount,
        Map<String, Long> ageGroupDistribution,
        Map<String, Long> jobDistribution,
        String summaryText,
        Instant generatedAt
) {}
