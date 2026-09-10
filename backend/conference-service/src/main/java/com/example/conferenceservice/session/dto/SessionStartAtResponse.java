package com.example.conferenceservice.session.dto;

import com.example.conferenceservice.session.entity.Session;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionStartAtResponse(
        UUID sessionId,
        LocalDateTime sessionStartAt
) {
    public static SessionStartAtResponse from(Session session) {
        return new SessionStartAtResponse(
                session.getId(),
                session.getSessionStartAt()
        );
    }
}
