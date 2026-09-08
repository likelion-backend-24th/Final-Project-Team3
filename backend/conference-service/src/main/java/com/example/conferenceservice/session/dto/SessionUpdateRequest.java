package com.example.conferenceservice.session.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SessionUpdateRequest(
    int capacity,

    @NotNull(message = "신청 시작 일시는 필수입니다.")
    LocalDateTime startAt,

    @NotNull(message = "신청 종료 일시는 필수입니다.")
    LocalDateTime endAt
) {}
