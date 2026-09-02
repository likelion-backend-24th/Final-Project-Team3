package com.example.conferenceservice.conference.dto;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;

import java.util.UUID;

public record ConferenceResponseDto (
        UUID id,
        UUID organizerId,
        String title,
        ConferenceStatus status,
        int capacity
)
{
    public static ConferenceResponseDto from(Conference conference) {
        return new ConferenceResponseDto(
                conference.getId(),
                conference.getOrganizerId(),
                conference.getTitle(),
                conference.getStatus(),
                conference.getCapacity()
        );
    }
}
