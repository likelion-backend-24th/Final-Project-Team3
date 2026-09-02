package com.example.conferenceservice.conference.dto;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.session.dto.SessionResponseDto;
import com.example.conferenceservice.session.entity.Session;

import java.util.List;
import java.util.UUID;

public record ConferenceDetailResponseDto (
        UUID id,
        UUID organizerId,
        String title,
        ConferenceStatus status,
        int capacity,
        List<SessionResponseDto> sessions
)
{
    public static ConferenceDetailResponseDto from(Conference conference, List<Session> sessions) {
        return new ConferenceDetailResponseDto(
                conference.getId(),
                conference.getOrganizerId(),
                conference.getTitle(),
                conference.getStatus(),
                conference.getCapacity(),
                sessions.stream().map(SessionResponseDto::from).toList()
        );
    }
}
