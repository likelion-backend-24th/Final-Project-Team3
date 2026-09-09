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

    // 도로명 주소 등 실제 위치 정보. 승인(APPROVED) 이후엔 참가자가 이미 이 주소를 보고 신청했을 수 있어
    // 변경할 수 없다 - locationDetail(교통편·주차·편의시설 등 부가 안내)은 주소 잠금과 무관하게 수정 가능하지만,
    // updateLocation() 호출 자체는 어떤 필드를 바꾸든 재승인이 필요하도록 상태를 PENDING으로 되돌린다.
    @Column
    private String location;

    @Lob
    @Column(name = "location_detail", columnDefinition = "TEXT")
    private String locationDetail;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "image_url")
    private String imageUrl;

    public boolean isPending() {
        return this.status == ConferenceStatus.PENDING;
    }

    public void approve() {
        this.status = ConferenceStatus.APPROVED;
    }

    public void reject(String reason) {
        this.status = ConferenceStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void updateDetails(String title, int capacity, LocalDateTime startAt, LocalDateTime endAt,
                               String description, String imageUrl) {
        this.title = title;
        this.capacity = capacity;
        this.startAt = startAt;
        this.endAt = endAt;
        this.description = description;
        this.imageUrl = imageUrl;
        markPendingForReapproval();
    }

    public void updateLocation(String location, String locationDetail) {
        this.location = location;
        this.locationDetail = locationDetail;
        markPendingForReapproval();
    }

    private void markPendingForReapproval() {
        this.status = ConferenceStatus.PENDING;
        this.rejectionReason = null;
    }

    public boolean isApproved() {
        return this.status == ConferenceStatus.APPROVED;
    }

    @PrePersist
    private void assignId() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }


}
