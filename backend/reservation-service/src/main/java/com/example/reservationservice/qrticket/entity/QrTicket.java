package com.example.reservationservice.qrticket.entity;

import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "qr_ticket")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QrTicket {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "reservation_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID reservationId;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group")
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    private Job job;

    @Builder
    public QrTicket(UUID reservationId, String code, AgeGroup ageGroup, Job job) {
        this.id = UUID.randomUUID();
        this.reservationId = reservationId;
        this.code = code;
        this.used = false;
        this.createdAt = LocalDateTime.now();
        this.ageGroup = ageGroup;
        this.job = job;
    }

    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
}