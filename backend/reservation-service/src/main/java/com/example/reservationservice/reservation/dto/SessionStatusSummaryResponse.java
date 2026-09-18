package com.example.reservationservice.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "세션별 신청 상태·체크인 건수 집계")
public record SessionStatusSummaryResponse(
        @Schema(description = "세션 ID")
        UUID sessionId,

        @Schema(description = "HOLD 상태 건수", example = "3")
        long holdCount,

        @Schema(description = "QUEUED(대기열) 상태 건수", example = "5")
        long queuedCount,

        @Schema(description = "CONFIRMED(결제완료) 상태 건수", example = "10")
        long confirmedCount,

        @Schema(description = "CANCELLED(취소) 상태 건수", example = "2")
        long cancelledCount,

        @Schema(description = "체크인(QR 사용) 완료 건수", example = "8")
        long checkedInCount
) {}