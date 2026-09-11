package com.example.conferenceservice.session.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record SessionCreateRequest(
    @NotBlank(message = "세션 이름은 필수입니다.")
    String title,

    int capacity,

    @NotNull(message = "신청 시작 일시는 필수입니다.")
    @Future(message = "신청 시작 일시는 현재 시각 이후여야 합니다.")
    LocalDateTime startAt,

    @NotNull(message = "신청 종료 일시는 필수입니다.")
    LocalDateTime endAt,

    @NotNull(message = "세션 진행 시작 일시는 필수입니다.")
    LocalDateTime sessionStartAt,

    @NotNull(message = "세션 진행 종료 일시는 필수입니다.")
    LocalDateTime sessionEndAt,

    @NotBlank(message = "장소는 필수입니다.")
    String location,

    @NotBlank(message = "발표자는 필수입니다.")
    String speaker,

    @NotNull(message = "참가 비용은 필수입니다.")
    @PositiveOrZero(message = "참가 비용은 0 이상이어야 합니다.")
    Integer price,

    @NotNull(message = "1인당 최대 신청 인원은 필수입니다.")
    @Positive(message = "1인당 최대 신청 인원은 1명 이상이어야 합니다.")
    Integer maxHeadcountPerApplication
) {}
