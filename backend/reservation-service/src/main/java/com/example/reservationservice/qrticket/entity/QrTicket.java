package com.example.reservationservice.qrticket.entity;

import com.example.reservationservice.qrticket.exception.QrTicketErrorCode;
import com.example.reservationservice.qrticket.exception.QrTicketException;
import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;

import com.github.f4b6a3.uuid.UuidCreator;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "QR 티켓 정보")
public class QrTicket {

    @Schema(description = "QR 티켓 ID")
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Schema(description = "예약 ID")
    @Column(name = "reservation_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID reservationId;

    @Schema(description = "QR 코드 문자열")
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Schema(description = "사용(입장 처리) 여부")
    @Column(nullable = false)
    private boolean used;

    @Schema(description = "입장 처리된 시각")
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Schema(description = "생성 시각")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Schema(description = "연령대")
    @Enumerated(EnumType.STRING)
    @Column(name = "age_group")
    private AgeGroup ageGroup;

    @Schema(description = "직무")
    @Enumerated(EnumType.STRING)
    private Job job;

    public void scan() {
        if (this.used) {
            throw new QrTicketException(QrTicketErrorCode.QR_TICKET_ALREADY_USED);
        }
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }

    @Builder
    public QrTicket(UUID reservationId, String code, AgeGroup ageGroup, Job job) {
        this.id = UuidCreator.getTimeOrderedEpoch();
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