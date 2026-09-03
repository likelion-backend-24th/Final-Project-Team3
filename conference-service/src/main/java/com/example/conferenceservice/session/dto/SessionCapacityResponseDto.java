package com.example.conferenceservice.session.dto;

import com.example.conferenceservice.session.entity.Session;

import java.util.UUID;

public record SessionCapacityResponseDto(
        UUID sessionId,
        int capacity
) {
    public static SessionCapacityResponseDto from(Session session) {
        return new SessionCapacityResponseDto(
                session.getId(),
                session.getCapacity()
        );
    }
}
