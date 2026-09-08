package com.example.conferenceservice.session.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SessionCreateRequest(
    @NotBlank(message = "세션 이름은 필수입니다.")
    String title,

    int capacity,

    @NotNull(message = "신청 시작 일시는 필수입니다.")
    @Future(message = "신청 시작 일시는 현재 시각 이후여야 합니다.")
    LocalDateTime startAt,

    @NotNull(message = "신청 종료 일시는 필수입니다.")
    LocalDateTime endAt
) {}
