package com.example.conferenceservice.session.dto;

import com.example.conferenceservice.session.entity.Session;

public record SessionResponse(
        java.util.UUID id,
        String title,
        int capacity
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getTitle(),
                session.getCapacity()
        );
    }
}
