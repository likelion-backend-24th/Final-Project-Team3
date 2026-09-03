package com.example.conferenceservice.conference.dto;

import com.example.conferenceservice.session.dto.SessionResponseDto;
import com.example.conferenceservice.session.entity.Session;

import java.util.List;
import java.util.UUID;

public record ConferenceCapacityResponseDto(
        UUID conferenceId,
        List<SessionResponseDto> sessions
) {
    public static ConferenceCapacityResponseDto from(UUID conferenceId, List<Session> sessions) {
        return new ConferenceCapacityResponseDto(
                conferenceId,
                sessions.stream().map(SessionResponseDto::from).toList()
        );
    }
}
