package com.example.conferenceservice.faq.dto;

import com.example.conferenceservice.faq.entity.ConferenceFaq;

import java.time.LocalDateTime;
import java.util.UUID;

public record FaqResponse(
        UUID id,
        String question,
        String answer,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FaqResponse from(ConferenceFaq faq) {
        return new FaqResponse(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getCreatedAt(),
                faq.getUpdatedAt()
        );
    }
}
