package com.example.conferenceservice.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;

import java.time.LocalDateTime;

@Schema(description = "세션 응답")
public record SessionResponse(
        @Schema(description = "세션 ID(UUID v7)") java.util.UUID id,
        @Schema(description = "소속 컨퍼런스 ID") java.util.UUID conferenceId,
        @Schema(description = "소속 컨퍼런스 이름") String conferenceTitle,
        @Schema(description = "세션 이름") String title,
        @Schema(description = "승인 상태: PENDING(승인 대기) / APPROVED(공개) / REJECTED(반려)") SessionStatus status,
        @Schema(description = "세션 정원") int capacity,
        @Schema(description = "참가 신청 접수 시작 일시") LocalDateTime startAt,
        @Schema(description = "참가 신청 접수 종료 일시") LocalDateTime endAt,
        @Schema(description = "세션 진행 시작 일시. 환불율 계산(7일 전 100% / 3~6일 전 50% / 3일 미만 불가)의 기준") LocalDateTime sessionStartAt,
        @Schema(description = "세션 진행 종료 일시") LocalDateTime sessionEndAt,
        @Schema(description = "세션 진행 장소") String location,
        @Schema(description = "발표자") String speaker,
        @Schema(description = "1인당 참가 가격(원). 0이면 무료 세션이라 결제 없이 확정된다") int price,
        @Schema(description = "1회 신청 시 신청 가능한 최대 인원") Integer maxHeadcountPerApplication,
        @Schema(description = "반려 사유. 반려된 경우에만 값이 있고 그 외에는 null") String rejectionReason
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getConference().getId(),
                session.getConference().getTitle(),
                session.getTitle(),
                session.getStatus(),
                session.getCapacity(),
                session.getStartAt(),
                session.getEndAt(),
                session.getSessionStartAt(),
                session.getSessionEndAt(),
                session.getLocation(),
                session.getSpeaker(),
                session.getPrice() == null ? 0 : session.getPrice(),
                session.getMaxHeadcountPerApplication(),
                session.getRejectReason()
        );
    }
}
