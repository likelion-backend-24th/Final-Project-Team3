package com.example.conferenceservice.session.entity;

import com.example.conferenceservice.conference.entity.Conference;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Entity
@Table(name = "session")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int capacity;

    // 신규 컬럼 - 기존 row가 있는 테이블에 NOT NULL DEFAULT 없이 추가하면 MySQL strict 모드에서
    // ddl-auto:update ALTER가 거부되므로 nullable로 두고 애플리케이션(Request 검증)에서 필수값을 보장한다.
    // startAt/endAt은 "신청" 기간이며, 세션이 실제로 열리는 일시는 sessionStartAt/sessionEndAt이다.
    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "session_start_at")
    private LocalDateTime sessionStartAt;

    @Column(name = "session_end_at")
    private LocalDateTime sessionEndAt;

    @Column(name = "location")
    private String location;

    @Column(name = "speaker")
    private String speaker;

    // 참가 비용(원). 기존 row는 마이그레이션 없이 컬럼만 추가되므로 null일 수 있어 Integer로 둔다.
    @Column(name = "price")
    private Integer price;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SessionStatus status = SessionStatus.PENDING;

    @Column(name = "reject_reason")
    private String rejectReason;

    @PrePersist
    private void assignId() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }

    public void updateSchedule(int capacity, LocalDateTime startAt, LocalDateTime endAt,
                                LocalDateTime sessionStartAt, LocalDateTime sessionEndAt,
                                String location, String speaker, Integer price) {
        this.capacity = capacity;
        this.startAt = startAt;
        this.endAt = endAt;
        this.sessionStartAt = sessionStartAt;
        this.sessionEndAt = sessionEndAt;
        this.location = location;
        this.speaker = speaker;
        this.price = price;
        this.status = SessionStatus.PENDING;
        this.rejectReason = null;
    }

    public boolean isPending() {
        return this.status == SessionStatus.PENDING;
    }

    public void approve() {
        this.status = SessionStatus.APPROVED;
    }

    public void reject(String reason) {
        this.status = SessionStatus.REJECTED;
        this.rejectReason = reason;
    }
}
