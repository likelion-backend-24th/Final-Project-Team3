package com.example.reservationservice.reservation.dto;

import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "내 예약 목록 조회 응답")
public record MyReservationResponse(
        @Schema(description = "예약 ID")
        UUID reservationId,

        @Schema(description = "세션 ID")
        UUID sessionId,

        @Schema(description = "예약 상태", example = "CONFIRMED")
        ReservationStatus status,

        @Schema(description = "신청 인원 수", example = "2")
        int headcount,

        @Schema(description = "신청 생성 시각")
        LocalDateTime createdAt
) {
    public static MyReservationResponse from(Reservation reservation) {
        return new MyReservationResponse(
                reservation.getId(),
                reservation.getSessionId(),
                reservation.getStatus(),
                reservation.getHeadcount(),
                reservation.getCreatedAt()
        );
    }
}