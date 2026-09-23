package com.example.conferenceservice.conference.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.entity.Session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "컨퍼런스 상세 응답(세션·주최자 이력 포함)")
public record ConferenceDetailResponse(
        @Schema(description = "컨퍼런스 ID(UUID v7)") UUID id,
        @Schema(description = "주최자 회원 ID") UUID organizerId,
        @Schema(description = "주최기관명") String organizerName,
        @Schema(description = "컨퍼런스 이름") String title,
        @Schema(description = "승인 상태: PENDING / APPROVED / REJECTED") ConferenceStatus status,
        @Schema(description = "컨퍼런스 전체 정원") int capacity,
        @Schema(description = "컨퍼런스 시작 일시") LocalDateTime startAt,
        @Schema(description = "컨퍼런스 종료 일시") LocalDateTime endAt,
        @Schema(description = "개최 장소") String location,
        @Schema(description = "교통편 안내(선택)") String transportation,
        @Schema(description = "주차 안내(선택)") String parkingInfo,
        @Schema(description = "편의시설 안내(선택)") String amenities,
        @Schema(description = "컨퍼런스 소개글") String description,
        @Schema(description = "목록 카드용 썸네일 이미지 URL(선택)") String thumbnailImageUrl,
        @Schema(description = "상세 페이지용 이미지 URL(선택)") String detailImageUrl,
        @Schema(description = "카테고리 태그 목록") List<String> tags,
        @Schema(description = "소속 세션 목록. 공개 조회는 APPROVED 세션만, 관리자 조회는 심사용으로 전체 상태를 포함한다") List<SessionResponse> sessions,
        @Schema(description = "주최자의 지난 승인(APPROVED) 컨퍼런스 수") int organizerPastConferenceCount,
        @Schema(description = "주최자 대표 후기 AI 요약 문장. 이력이 없으면 null") String organizerRepresentativeSummary,
        @Schema(description = "승인 심사용 증명 파일 첨부 여부") boolean proofFileAttached
){
    public static ConferenceDetailResponse from(Conference conference, List<Session> sessions, List<String> tags,
                                                int organizerPastConferenceCount, String organizerRepresentativeSummary) {
        return new ConferenceDetailResponse(
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
                ConferenceImageUrls.thumbnailOf(conference),
                ConferenceImageUrls.detailOf(conference),
                tags,
                sessions.stream().map(SessionResponse::from).toList(),
                organizerPastConferenceCount,
                organizerRepresentativeSummary,
                conference.hasProofFile()
        );
    }
}
