package com.example.conferenceservice.conference.dto;

import jakarta.validation.constraints.NotBlank;

public record ConferenceLocationUpdateRequest(
    @NotBlank(message = "장소는 필수입니다.")
    String location,

    String transportation,

    String parkingInfo,

    String amenities
) {}
