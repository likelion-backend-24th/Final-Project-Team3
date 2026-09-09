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
        LocalDateTime endAt,
        LocalDateTime sessionStartAt,
        LocalDateTime sessionEndAt,
        String location,
        String speaker,
        int price
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getConference().getId(),
                session.getConference().getTitle(),
                session.getTitle(),
                session.getCapacity(),
                session.getStartAt(),
                session.getEndAt(),
                session.getSessionStartAt(),
                session.getSessionEndAt(),
                session.getLocation(),
                session.getSpeaker(),
                session.getPrice() == null ? 0 : session.getPrice()
        );
    }
}
