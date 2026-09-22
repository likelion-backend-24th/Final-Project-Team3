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

// session_id+position 유니크 제약이 없으면 동시 등록 시 findMaxPositionBySessionId()로 읽은 다음
// +1 해서 쓰는 read-then-write 구간이 겹쳐 같은 순번이 여러 건에 배정된다(순번 중복·누락).
// registerToQueueWithRetry()의 "충돌 시 재시도" 로직은 이 제약이 DataIntegrityViolationException을
// 던져줘야 동작하므로, 제약 자체가 그 재시도 로직의 전제 조건이다.
@Entity
@Table(name = "waiting_queue", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "position"}))
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
