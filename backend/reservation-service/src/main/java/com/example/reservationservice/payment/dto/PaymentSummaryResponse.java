package com.example.reservationservice.payment.dto;

/**
 * 세션별 결제 정산 집계 응답.
 * Conference-Service가 /internal/sessions/payment-summary 호출 시 반환됨.
 *
 * @param totalRevenue 결제완료(CONFIRMED) 건 금액 합계
 * @param refundedAmount 취소(CANCELLED)된 건의 환불 금액 합계
 * @param netRevenue 순매출 (totalRevenue - refundedAmount)
 * @param confirmedCount 결제완료 건수
 * @param cancelledCount 취소 건수
 */
public record PaymentSummaryResponse(
        int totalRevenue,
        int refundedAmount,
        int netRevenue,
        int confirmedCount,
        int cancelledCount
) {}