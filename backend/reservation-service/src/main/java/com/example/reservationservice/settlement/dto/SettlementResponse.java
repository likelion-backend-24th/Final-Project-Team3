package com.example.reservationservice.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "통합 정산 대시보드 집계 결과")
public record SettlementResponse(
        @Schema(description = "기간 내 결제 완료 건의 결제 금액 합계(원). 취소·환불 건은 제외", example = "1250000") long totalAmount
) {}
