package com.example.reservationservice.reservation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "session_capacity_lock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionCapacityLock {

    @Id
    @Column(name = "session_id", columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @Column(name = "current_active", nullable = false)
    private Integer currentActive;

    public SessionCapacityLock(UUID sessionId) {
        this.sessionId = sessionId;
        this.currentActive = 0;
    }

    public void increase(int headcount) {
        this.currentActive += headcount;
    }
}