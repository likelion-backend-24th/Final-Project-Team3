package com.example.conferenceservice.conference.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ConferenceRequest(
    @NotBlank(message = "컨퍼런스 이름은 필수입니다.")
    String title,

    @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
    int capacity
    ){
}
