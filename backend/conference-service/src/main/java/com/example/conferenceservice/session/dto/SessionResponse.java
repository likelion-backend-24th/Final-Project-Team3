package com.example.conferenceservice.session.dto;

import com.example.conferenceservice.session.entity.Session;

import java.time.LocalDateTime;

public record SessionResponse(
        java.util.UUID id,
        java.util.UUID conferenceId,
        String conferenceTitle,
        String title,
        int capacity,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getConference().getId(),
                session.getConference().getTitle(),
                session.getTitle(),
                session.getCapacity(),
                session.getStartAt(),
                session.getEndAt()
        );
    }
}
