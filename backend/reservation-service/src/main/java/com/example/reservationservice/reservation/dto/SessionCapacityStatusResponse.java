package com.example.reservationservice.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "세션 정원 현황 조회 응답")
public record SessionCapacityStatusResponse(
        @Schema(description = "세션 ID")
        UUID sessionId,

        @Schema(description = "세션 정원", example = "100")
        int capacity,

        @Schema(description = "확정(결제완료)된 인원 수", example = "80")
        int confirmedCount,

        @Schema(description = "잔여 좌석 수", example = "20")
        int remaining
) {}