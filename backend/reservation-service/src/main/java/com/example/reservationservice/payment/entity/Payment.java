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

    public void recordRefund(Integer refundedAmount) {
        this.refundedAmount = refundedAmount;
        this.refundedAt = LocalDateTime.now();
    }
}