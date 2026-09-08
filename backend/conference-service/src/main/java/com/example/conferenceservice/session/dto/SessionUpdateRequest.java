package com.example.conferenceservice.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SessionUpdateRequest(
    @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
    int capacity,

    @NotNull(message = "신청 시작 일시는 필수입니다.")
    LocalDateTime startAt,

    @NotNull(message = "신청 종료 일시는 필수입니다.")
    LocalDateTime endAt
) {}
