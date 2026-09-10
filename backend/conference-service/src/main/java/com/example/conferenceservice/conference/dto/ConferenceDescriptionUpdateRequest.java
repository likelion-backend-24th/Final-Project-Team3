package com.example.conferenceservice.conference.dto;

import jakarta.validation.constraints.NotBlank;

public record ConferenceDescriptionUpdateRequest(
    @NotBlank(message = "소개글은 필수입니다.")
    String description
) {}
