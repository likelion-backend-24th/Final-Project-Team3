package com.example.conferenceservice.conference.entity;

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
@Table(name = "conference")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Conference {
    @Id
    private UUID id;

    // 논리적 FK - Member-Service의 organizer PK를 값으로만 보관
    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    // Member-Service를 호출하지 않도록 신청 시점의 이름을 스냅샷으로 보관
    // nullable(false)로 두면 ddl-auto: update가 기존 row가 있는 테이블에 컬럼을 추가할 때
    // DEFAULT 없는 NOT NULL ALTER가 MySQL strict 모드에서 거부됨 - 필수값 검증은 ConferenceRequest에서 담당
    @Column(name = "organizer_name")
    private String organizerName;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConferenceStatus status; // 신청, 승인, 반려

    @Column(nullable = false)
    private int capacity;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column
    private String location;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @PrePersist
    private void assignId() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }


}
