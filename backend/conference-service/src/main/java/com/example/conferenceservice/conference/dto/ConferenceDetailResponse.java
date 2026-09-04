package com.example.conferenceservice.conference.dto;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.entity.Session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConferenceDetailResponse(
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
        List<String> tags,
        List<SessionResponse> sessions
)
{
    public static ConferenceDetailResponse from(Conference conference, List<Session> sessions, List<String> tags) {
        return new ConferenceDetailResponse(
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
                tags,
                sessions.stream().map(SessionResponse::from).toList()
        );
    }
}
