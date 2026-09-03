package com.example.reservationservice.reservation.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "session_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(nullable = false)
    private Integer headcount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Reservation(UUID sessionId, UUID memberId, Integer headcount) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.sessionId = sessionId;
        this.memberId = memberId;
        this.headcount = headcount;
        this.status = ReservationStatus.HOLD;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsQueued() {
        this.status = ReservationStatus.QUEUED;
    }
}
