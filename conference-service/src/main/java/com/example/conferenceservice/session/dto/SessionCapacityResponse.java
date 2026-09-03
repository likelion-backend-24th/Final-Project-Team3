package com.example.conferenceservice.session.dto;

import com.example.conferenceservice.session.entity.Session;

import java.util.UUID;

public record SessionCapacityResponse(
        UUID sessionId,
        int capacity
) {
    public static SessionCapacityResponse from(Session session) {
        return new SessionCapacityResponse(
                session.getId(),
                session.getCapacity()
        );
    }
}
