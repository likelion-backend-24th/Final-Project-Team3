package com.example.reservationservice.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대기열 순번 및 예상 대기 시간 조회 응답")
public record QueuePositionResponse(
        @Schema(description = "현재 대기열 순번", example = "3")
        int position,

        @Schema(description = "늦어도 이 시간(분) 안에 자리가 날지 결과가 나온다 — 결제 대기 중인 좌석의 만료 시각 기준. "
                + "결제 대기 좌석만으로 부족하면(대부분 결제 완료) 취소를 기다려야 해서 null",
                example = "8", nullable = true)
        Integer estimatedWaitMinutes
) {}
