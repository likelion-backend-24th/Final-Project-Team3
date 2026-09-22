package com.example.conferenceservice.session.dto;

import com.example.conferenceservice.session.entity.Session;

import java.util.UUID;

public record SessionConferenceIdResponse(UUID sessionId, UUID conferenceId) {
    public static SessionConferenceIdResponse from(Session session) {
        return new SessionConferenceIdResponse(session.getId(), session.getConference().getId());
    }
}