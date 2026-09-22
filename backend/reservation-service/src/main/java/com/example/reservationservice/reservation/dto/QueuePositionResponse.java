package com.example.reservationservice.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대기열 순번 및 예상 대기 시간 조회 응답")
public record QueuePositionResponse(
        @Schema(description = "현재 대기열 순번", example = "3")
        int position,

        @Schema(description = "예상 대기 시간(분)", example = "15")
        int estimatedWaitMinutes
) {}