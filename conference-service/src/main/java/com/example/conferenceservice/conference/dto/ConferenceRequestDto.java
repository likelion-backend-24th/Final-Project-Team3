package com.example.conferenceservice.conference.dto;

import jakarta.validation.constraints.NotBlank;

public record ConferenceRequestDto (
    @NotBlank(message = "컨퍼런스 이름은 필수입니다.")
    String title,

    int capacity
    ){
}
