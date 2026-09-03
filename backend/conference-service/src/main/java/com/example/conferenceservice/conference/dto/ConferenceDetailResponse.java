package com.example.conferenceservice.conference.dto;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.entity.Session;

import java.util.List;
import java.util.UUID;

public record ConferenceDetailResponse(
        UUID id,
        UUID organizerId,
        String title,
        ConferenceStatus status,
        int capacity,
        List<SessionResponse> sessions
)
{
    public static ConferenceDetailResponse from(Conference conference, List<Session> sessions) {
        return new ConferenceDetailResponse(
                conference.getId(),
                conference.getOrganizerId(),
                conference.getTitle(),
                conference.getStatus(),
                conference.getCapacity(),
                sessions.stream().map(SessionResponse::from).toList()
        );
    }
}
