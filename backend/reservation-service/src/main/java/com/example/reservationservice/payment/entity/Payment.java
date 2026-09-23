package com.example.reservationservice.payment.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "reservation_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID reservationId;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "refunded_amount")
    private Integer refundedAmount;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Builder
    public Payment(UUID reservationId, Integer amount, String paymentMethod) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.reservationId = reservationId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paidAt = LocalDateTime.now();
    }

    // 누적: 한 예약을 여러 번에 걸쳐 부분 환불(개별 인원 취소)할 수 있으므로 덮어쓰지 않고 더한다.
    // 전체 취소는 이 메서드를 한 번만 부르므로 기존 동작과 동일하다(null이던 값에 전액이 더해짐).
    public void recordRefund(Integer additionalRefundedAmount) {
        this.refundedAmount = (this.refundedAmount == null ? 0 : this.refundedAmount) + additionalRefundedAmount;
        this.refundedAt = LocalDateTime.now();
    }
}