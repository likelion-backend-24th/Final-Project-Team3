package com.example.reservationservice.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 세션별 대기열 다음 순번 카운터. session_capacity_lock과 같은 패턴(세션당 행 1개 + 원자적
// UPDATE)으로 순번을 배정한다 — read-then-write(findMax+1)와 달리 경쟁 상태가 생기지 않아
// 재시도가 필요 없다.
@Entity
@Table(name = "queue_position_counter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QueuePositionCounter {

    @Id
    @Column(name = "session_id", columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @Column(name = "next_position", nullable = false)
    private Integer nextPosition;

    public QueuePositionCounter(UUID sessionId) {
        this.sessionId = sessionId;
        this.nextPosition = 1;
    }
}
