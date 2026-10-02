package com.example.conferenceservice.session.dto;

import com.example.conferenceservice.session.entity.Session;

import java.util.UUID;

public record SessionOrganizerIdResponse(UUID sessionId, UUID organizerId) {
    public static SessionOrganizerIdResponse from(Session session) {
        return new SessionOrganizerIdResponse(session.getId(), session.getConference().getOrganizerId());
    }
}