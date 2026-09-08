package com.example.conferenceservice.session.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectSessionRequest(
        @NotBlank(message = "반려 사유는 필수입니다.")
        String reason
) {
}
