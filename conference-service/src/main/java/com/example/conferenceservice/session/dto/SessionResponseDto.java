package com.example.conferenceservice.session.dto;

import com.example.conferenceservice.session.entity.Session;

public record SessionResponseDto(
        Long id,
        String title,
        int capacity
) {
    public static SessionResponseDto from(Session session) {
        return new SessionResponseDto(
                session.getId(),
                session.getTitle(),
                session.getCapacity()
        );
    }
}
