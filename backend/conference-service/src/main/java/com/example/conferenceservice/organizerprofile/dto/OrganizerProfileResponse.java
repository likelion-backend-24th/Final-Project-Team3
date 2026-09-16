package com.example.conferenceservice.organizerprofile.dto;

import java.util.List;
import java.util.UUID;

public record OrganizerProfileResponse(
        UUID organizerId,
        String organizerName,
        List<PastConferenceResponse> pastConferences
) {}
