package com.example.conferenceservice.conference.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectConferenceRequest(
        @NotBlank(message = "반려 사유는 필수입니다.")
        String reason
) {
}
