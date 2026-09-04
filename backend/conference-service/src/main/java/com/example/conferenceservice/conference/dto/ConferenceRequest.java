package com.example.conferenceservice.conference.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record ConferenceRequest(
    @NotBlank(message = "주최자 이름은 필수입니다.")
    String organizerName,

    @NotBlank(message = "컨퍼런스 이름은 필수입니다.")
    String title,

    @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
    int capacity,

    @NotNull(message = "시작 일시는 필수입니다.")
    @Future(message = "시작 일시는 현재 시각 이후여야 합니다.")
    LocalDateTime startAt,

    @NotNull(message = "종료 일시는 필수입니다.")
    LocalDateTime endAt,

    @NotBlank(message = "장소는 필수입니다.")
    String location,

    String description,

    List<@NotBlank(message = "태그는 빈 값일 수 없습니다.") String> tags
    ){
}
