package com.example.conferenceservice.conference.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "컨퍼런스 요약 응답")
public record ConferenceResponse(
        @Schema(description = "컨퍼런스 ID(UUID v7)") UUID id,
        @Schema(description = "주최자 회원 ID") UUID organizerId,
        @Schema(description = "주최기관명") String organizerName,
        @Schema(description = "컨퍼런스 이름") String title,
        @Schema(description = "승인 상태: PENDING(승인 대기) / APPROVED(공개) / REJECTED(반려)") ConferenceStatus status,
        @Schema(description = "컨퍼런스 전체 정원(세션 정원이 이를 넘을 수 없다)") int capacity,
        @Schema(description = "컨퍼런스 시작 일시") LocalDateTime startAt,
        @Schema(description = "컨퍼런스 종료 일시") LocalDateTime endAt,
        @Schema(description = "개최 장소. 승인 후에는 변경할 수 없다") String location,
        @Schema(description = "교통편 안내(선택)") String transportation,
        @Schema(description = "주차 안내(선택)") String parkingInfo,
        @Schema(description = "편의시설 안내(선택)") String amenities,
        @Schema(description = "컨퍼런스 소개글") String description,
        @Schema(description = "대표 이미지 URL(선택)") String imageUrl,
        @Schema(description = "카테고리 태그 목록(신청 시 최소 1개)") List<String> tags,
        @Schema(description = "공개(APPROVED)된 세션 수. 목록 응답에서는 0일 수 있다") long sessionCount,
        @Schema(description = "반려 사유. 반려된 경우에만 값이 있고 그 외에는 null") String rejectionReason,
        @Schema(description = "승인 심사용 증명 파일 첨부 여부. 파일 자체는 이 응답에 없다") boolean proofFileAttached
)
{
    public static ConferenceResponse from(Conference conference) {
        return from(conference, 0, List.of());
    }

    public static ConferenceResponse from(Conference conference, long sessionCount) {
        return from(conference, sessionCount, List.of());
    }

    public static ConferenceResponse from(Conference conference, long sessionCount, List<String> tags) {
        return new ConferenceResponse(
                conference.getId(),
                conference.getOrganizerId(),
                conference.getOrganizerName(),
                conference.getTitle(),
                conference.getStatus(),
                conference.getCapacity(),
                conference.getStartAt(),
                conference.getEndAt(),
                conference.getLocation(),
                conference.getTransportation(),
                conference.getParkingInfo(),
                conference.getAmenities(),
                conference.getDescription(),
                conference.getImageUrl(),
                tags,
                sessionCount,
                conference.getRejectionReason(),
                conference.hasProofFile()
        );
    }
}
