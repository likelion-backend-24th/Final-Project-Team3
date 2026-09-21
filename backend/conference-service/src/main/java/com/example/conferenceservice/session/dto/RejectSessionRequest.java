package com.example.conferenceservice.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "세션 반려 요청")
public record RejectSessionRequest(
        @Schema(description = "반려 사유. 주최자에게 그대로 표시된다", example = "증빙 자료가 부족합니다")
        @NotBlank(message = "반려 사유는 필수입니다.")
        String reason
) {
}
