package com.example.reservationservice.reservation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "active_reservation_lock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(ActiveReservationLockId.class)
public class ActiveReservationLock {

    @Id
    @Column(name = "session_id", columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @Id
    @Column(name = "member_id", columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(name = "reservation_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID reservationId;

    public ActiveReservationLock(UUID sessionId, UUID memberId, UUID reservationId) {
        this.sessionId = sessionId;
        this.memberId = memberId;
        this.reservationId = reservationId;
    }
}