package com.example.conferenceservice.conference.dto;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;

import java.util.UUID;

public record ConferenceResponse(
        UUID id,
        UUID organizerId,
        String title,
        ConferenceStatus status,
        int capacity
)
{
    public static ConferenceResponse from(Conference conference) {
        return new ConferenceResponse(
                conference.getId(),
                conference.getOrganizerId(),
                conference.getTitle(),
                conference.getStatus(),
                conference.getCapacity()
        );
    }
}
