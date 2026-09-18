package com.example.conferenceservice.session.entity;

import com.example.conferenceservice.conference.entity.Conference;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
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

    // 1인당(1회 신청당) 최대 신청 인원. 프론트에서 고정값(4명)으로 막던 것을 주최자가 세션별로 설정하도록 이관.
    // 기존 row는 마이그레이션 없이 컬럼만 추가되므로 null일 수 있어 Integer로 둔다.
    @Column(name = "max_headcount_per_application")
    private Integer maxHeadcountPerApplication;

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
                                String location, String speaker, Integer price,
                                Integer maxHeadcountPerApplication) {
        // 값이 실제로 하나라도 바뀔 때만 재승인(PENDING) 대상으로 삼는다 - 동일 값 재제출로
        // APPROVED 세션이 이유 없이 승인 대기 상태로 되돌아가는 것을 막는다.
        // 초 단위로 truncate 후 비교 - DB 컬럼(MySQL DATETIME)이 나노초를 버리므로, 저장 전/후 값을
        // 그대로 비교하면 프론트가 안 보내는 미세 정밀도 차이 때문에 "안 바뀐 값"이 바뀐 것으로 오판될 수 있다.
        boolean changed = this.capacity != capacity
                || !Objects.equals(truncateToSeconds(this.startAt), truncateToSeconds(startAt))
                || !Objects.equals(truncateToSeconds(this.endAt), truncateToSeconds(endAt))
                || !Objects.equals(truncateToSeconds(this.sessionStartAt), truncateToSeconds(sessionStartAt))
                || !Objects.equals(truncateToSeconds(this.sessionEndAt), truncateToSeconds(sessionEndAt))
                || !Objects.equals(this.location, location)
                || !Objects.equals(this.speaker, speaker)
                || !Objects.equals(this.price, price)
                || !Objects.equals(this.maxHeadcountPerApplication, maxHeadcountPerApplication);

        this.capacity = capacity;
        this.startAt = startAt;
        this.endAt = endAt;
        this.sessionStartAt = sessionStartAt;
        this.sessionEndAt = sessionEndAt;
        this.location = location;
        this.speaker = speaker;
        this.price = price;
        this.maxHeadcountPerApplication = maxHeadcountPerApplication;
        // REJECTED는 값이 안 바뀌었어도 재승인 대기로 돌린다 - 그렇지 않으면 반려된 세션은
        // 아무 필드도 안 건드리는 한 영원히 REJECTED에 갇혀 재심사를 받을 방법이 없어진다.
        if (changed || this.status == SessionStatus.REJECTED) {
            this.status = SessionStatus.PENDING;
            this.rejectReason = null;
        }
    }

    private static LocalDateTime truncateToSeconds(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.SECONDS);
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
