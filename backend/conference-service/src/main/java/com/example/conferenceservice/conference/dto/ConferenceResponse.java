package com.example.conferenceservice.conference.dto;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConferenceResponse(
        UUID id,
        UUID organizerId,
        String organizerName,
        String title,
        ConferenceStatus status,
        int capacity,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String location,
        String description,
        String imageUrl,
        long sessionCount
)
{
    public static ConferenceResponse from(Conference conference) {
        return from(conference, 0);
    }

    public static ConferenceResponse from(Conference conference, long sessionCount) {
        return new ConferenceResponse(
                conference.getId(),
                conference.getOrganizerId(),
                conference.getOrganizerName(),
                conference.getTitle(),
                conference.getStatus(),
                conference.getCapacity(),
                conference.getStartAt(),
                conference.getEndAt(),
                conference.getLocation(),
                conference.getDescription(),
                conference.getImageUrl(),
                sessionCount
        );
    }
}
