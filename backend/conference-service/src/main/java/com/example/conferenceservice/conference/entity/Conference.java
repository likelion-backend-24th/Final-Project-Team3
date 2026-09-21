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
    // 변경할 수 없다([장소-수정불가] 규칙) - transportation/parkingInfo/amenities는 주소 잠금과 무관하게 항상 수정 가능하다.
    @Column
    private String location;

    @Column(name = "transportation")
    private String transportation;

    @Column(name = "parking_info")
    private String parkingInfo;

    @Column(name = "amenities")
    private String amenities;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // 목록 카드용 썸네일과 상세 페이지용 이미지를 각각 리사이징해서 저장한 파일명(FileStorageService.storeImage 결과)
    @Column(name = "thumbnail_image_name")
    private String thumbnailImageName;

    @Column(name = "detail_image_name")
    private String detailImageName;

    // 등록 신청 시 첨부한 컨퍼런스 증명 파일의 저장 파일명({UUID}_{원본파일명}). 관리자 승인 심사용.
    @Column(name = "proof_file_name")
    private String proofFileName;

    public boolean hasProofFile() {
        return this.proofFileName != null;
    }

    public void attachProofFile(String proofFileName) {
        this.proofFileName = proofFileName;
    }

    public void attachImages(String thumbnailImageName, String detailImageName) {
        this.thumbnailImageName = thumbnailImageName;
        this.detailImageName = detailImageName;
    }

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

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateLocation(String location, String transportation, String parkingInfo, String amenities) {
        this.location = location;
        this.transportation = transportation;
        this.parkingInfo = parkingInfo;
        this.amenities = amenities;
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
