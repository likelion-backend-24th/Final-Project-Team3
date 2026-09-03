package com.example.reservationservice.reservation.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.query.sql.internal.ParameterRecognizerImpl;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "waiting_queue")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingQueue {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId; // 같은 Reservation-Service 내 FK 가능

    @Column(name = "session_id", nullable = false)
    private UUID sessionId; // Conference-Service 소유, 논리 참조

    @Column(name = "member_id", nullable = false)
    private UUID memberId;       // Member-Service 소유, 논리 참조

    @Column(nullable = false)
    private Integer position;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Builder
    public WaitingQueue(UUID reservationId, UUID sessionId, UUID memberId, Integer position) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.reservationId = reservationId;
        this.sessionId = sessionId;
        this.memberId = memberId;
        this.position = position;
        this.joinedAt = LocalDateTime.now();
    }
}
