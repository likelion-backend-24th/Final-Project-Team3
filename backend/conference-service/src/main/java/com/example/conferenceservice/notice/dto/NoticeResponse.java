package com.example.conferenceservice.notice.dto;

import com.example.conferenceservice.notice.entity.ConferenceNotice;

import java.time.LocalDateTime;
import java.util.UUID;

public record NoticeResponse(
        UUID id,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeResponse from(ConferenceNotice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
