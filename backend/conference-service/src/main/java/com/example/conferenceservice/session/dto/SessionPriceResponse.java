package com.example.conferenceservice.session.dto;

import com.example.conferenceservice.session.entity.Session;

import java.util.UUID;

public record SessionPriceResponse(
        UUID sessionId,
        Integer price
) {
    public static SessionPriceResponse from(Session session) {
        return new SessionPriceResponse(
                session.getId(),
                session.getPrice()
        );
    }
}
