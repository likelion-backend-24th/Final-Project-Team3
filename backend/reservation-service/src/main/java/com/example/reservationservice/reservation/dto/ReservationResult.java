// ReservationResult.java 전체를 record로 교체
package com.example.reservationservice.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "세션 신청(홀드·대기열 등록) 결과")
public record ReservationResult(
        @Schema(description = "생성된 예약 ID")
        UUID reservationId,

        @Schema(description = "신청 상태", example = "HOLD")
        String status,

        @Schema(description = "대기열 순번. QUEUED일 때만 값 있음", example = "3", nullable = true)
        Integer queuePosition
) {
    public static ReservationResult hold(UUID reservationId) {
        return new ReservationResult(reservationId, "HOLD", null);
    }

    public static ReservationResult queued(UUID reservationId, int position) {
        return new ReservationResult(reservationId, "QUEUED", position);
    }
}