package com.example.conferenceservice.operationstatus.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.common.security.OwnerScopeGuard;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.operationstatus.client.ReservationServiceClient;
import com.example.conferenceservice.operationstatus.client.ReservationServiceUnavailableException;
import com.example.conferenceservice.operationstatus.dto.ConferenceOperationStatusResponse;
import com.example.conferenceservice.operationstatus.dto.ConferenceOperationStatusResponse.SessionOperationStatus;
import com.example.conferenceservice.operationstatus.exception.OperationStatusErrorCode;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 세션별 신청·입장 현황 조회는 매 세션마다 Reservation-Service로 순차 HTTP 호출을 수행하므로,
 * 이 호출들을 하나의 DB 트랜잭션 안에 묶으면 외부 호출이 지연될 때 커넥션 풀을 오래 점유하게 된다.
 * 따라서 이 메서드에는 의도적으로 @Transactional을 붙이지 않는다 - 아래 리포지토리 조회는
 * Spring Data가 각 호출마다 자체 짧은 트랜잭션으로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConferenceOperationStatusService {

    private final ConferenceRepository conferenceRepository;
    private final SessionRepository sessionRepository;
    private final ReservationServiceClient reservationServiceClient;

    public ConferenceOperationStatusResponse getOperationStatus(UUID conferenceId, UUID requesterId) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
        OwnerScopeGuard.verify(requesterId, conference.getOrganizerId(), ConferenceErrorCode.CONFERENCE_ACCESS_DENIED);

        List<Session> sessions = sessionRepository.findByConferenceId(conferenceId).stream()
                .sorted(Comparator.comparing(Session::getSessionStartAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<SessionOperationStatus> statuses = sessions.stream()
                .map(this::toOperationStatus)
                .toList();

        return new ConferenceOperationStatusResponse(conferenceId, statuses);
    }

    private SessionOperationStatus toOperationStatus(Session session) {
        try {
            ReservationServiceClient.SessionStatusSummaryResponse summary =
                    reservationServiceClient.getStatusSummary(session.getId());
            return new SessionOperationStatus(
                    session.getId(),
                    session.getTitle(),
                    summary.holdCount(),
                    summary.queuedCount(),
                    summary.confirmedCount(),
                    summary.cancelledCount(),
                    summary.checkedInCount());
        } catch (ReservationServiceUnavailableException e) {
            log.warn("Reservation-Service 응답 실패로 신청·입장 현황 조회를 중단합니다: sessionId={}", session.getId(), e);
            throw new BusinessException(OperationStatusErrorCode.RESERVATION_SERVICE_UNAVAILABLE);
        }
    }
}
