package com.example.conferenceservice.session.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.common.security.OwnerScopeGuard;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.operationstatus.client.ReservationServiceClient;
import com.example.conferenceservice.operationstatus.client.ReservationServiceUnavailableException;
import com.example.conferenceservice.session.dto.RejectSessionRequest;
import com.example.conferenceservice.session.dto.SessionCapacityResponse;
import com.example.conferenceservice.session.dto.SessionCreateRequest;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.dto.SessionStartAtResponse;
import com.example.conferenceservice.session.dto.SessionUpdateRequest;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
import com.example.conferenceservice.session.exception.SessionErrorCode;
import com.example.conferenceservice.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final ConferenceRepository conferenceRepository;
    private final ReservationServiceClient reservationServiceClient;

    @Transactional(readOnly = true)
    public Page<Session> getPendingSessions(Pageable pageable) {
        return sessionRepository.findByStatus(SessionStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public SessionCapacityResponse getCapacity(UUID sessionId) {
        Session session = sessionRepository.findByIdAndConference_Status(sessionId, ConferenceStatus.APPROVED)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));
        return SessionCapacityResponse.from(session);
    }

    @Transactional(readOnly = true)
    public SessionStartAtResponse getStartAt(UUID sessionId) {
        Session session = sessionRepository.findByIdAndConference_Status(sessionId, ConferenceStatus.APPROVED)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));
        return SessionStartAtResponse.from(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getSessionsByConference(UUID conferenceId, UUID requesterId) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
        OwnerScopeGuard.verify(requesterId, conference.getOrganizerId(), SessionErrorCode.SESSION_ACCESS_DENIED);
        return sessionRepository.findByConferenceId(conferenceId).stream()
                .map(SessionResponse::from)
                .toList();
    }

    @Transactional
    public SessionResponse createSession(UUID conferenceId, SessionCreateRequest request, UUID requesterId) {
        validateSchedule(request.capacity(), request.startAt(), request.endAt(),
                request.sessionStartAt(), request.sessionEndAt(), request.maxHeadcountPerApplication());

        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
        OwnerScopeGuard.verify(requesterId, conference.getOrganizerId(), SessionErrorCode.SESSION_ACCESS_DENIED);
        if (conference.getStatus() != ConferenceStatus.APPROVED) {
            throw new BusinessException(SessionErrorCode.CONFERENCE_NOT_APPROVED);
        }
        validateWithinConferencePeriod(request.sessionStartAt(), request.sessionEndAt(), conference);
        validateCapacityWithinConference(conference, request.capacity());

        Session session = Session.builder()
                .conference(conference)
                .title(request.title())
                .capacity(request.capacity())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .sessionStartAt(request.sessionStartAt())
                .sessionEndAt(request.sessionEndAt())
                .location(request.location())
                .speaker(request.speaker())
                .price(request.price())
                .maxHeadcountPerApplication(request.maxHeadcountPerApplication())
                .build();
        Session saved = sessionRepository.save(session);
        return SessionResponse.from(saved);
    }

    @Transactional
    public SessionResponse updateSession(UUID sessionId, SessionUpdateRequest request, UUID requesterId) {
        validateSchedule(request.capacity(), request.startAt(), request.endAt(),
                request.sessionStartAt(), request.sessionEndAt(), request.maxHeadcountPerApplication());

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));
        OwnerScopeGuard.verify(requesterId, session.getConference().getOrganizerId(), SessionErrorCode.SESSION_ACCESS_DENIED);
        if (session.getConference().getStatus() != ConferenceStatus.APPROVED) {
            throw new BusinessException(SessionErrorCode.CONFERENCE_NOT_APPROVED);
        }
        validateWithinConferencePeriod(request.sessionStartAt(), request.sessionEndAt(), session.getConference());
        validateCapacityWithinConference(session.getConference(), request.capacity());
        validateAgainstActiveReservations(session, request);
        session.updateSchedule(request.capacity(), request.startAt(), request.endAt(),
                request.sessionStartAt(), request.sessionEndAt(),
                request.location(), request.speaker(), request.price(),
                request.maxHeadcountPerApplication());
        return SessionResponse.from(session);
    }

    // 승인 전 세션은 예약을 받을 수 없으므로 확정 예약이 존재할 수 없다 - 불필요한 외부 호출을 피한다.
    // 정원 축소·진행 일정 변경처럼 이미 확정된 예약자에게 영향을 줄 수 있는 수정만 예약 현황과 대조한다.
    private void validateAgainstActiveReservations(Session session, SessionUpdateRequest request) {
        if (session.getStatus() != SessionStatus.APPROVED) {
            return;
        }
        boolean capacityReduced = request.capacity() < session.getCapacity();
        boolean scheduleChanged = !Objects.equals(request.sessionStartAt(), session.getSessionStartAt())
                || !Objects.equals(request.sessionEndAt(), session.getSessionEndAt());
        if (!capacityReduced && !scheduleChanged) {
            return;
        }

        long confirmedCount = fetchConfirmedCount(session.getId());
        if (confirmedCount == 0) {
            return;
        }
        if (capacityReduced && request.capacity() < confirmedCount) {
            throw new BusinessException(SessionErrorCode.SESSION_CAPACITY_BELOW_CONFIRMED_COUNT);
        }
        if (scheduleChanged) {
            throw new BusinessException(SessionErrorCode.SESSION_SCHEDULE_CHANGE_WITH_ACTIVE_RESERVATIONS);
        }
    }

    private long fetchConfirmedCount(UUID sessionId) {
        try {
            return reservationServiceClient.getStatusSummary(sessionId).confirmedCount();
        } catch (ReservationServiceUnavailableException e) {
            throw new BusinessException(SessionErrorCode.SESSION_RESERVATION_SERVICE_UNAVAILABLE);
        }
    }

    private void validateSchedule(int capacity, LocalDateTime startAt, LocalDateTime endAt,
                                   LocalDateTime sessionStartAt, LocalDateTime sessionEndAt,
                                   Integer maxHeadcountPerApplication) {
        if (capacity <= 0) {
            throw new BusinessException(SessionErrorCode.INVALID_SESSION_CAPACITY);
        }
        if (!endAt.isAfter(startAt)) {
            throw new BusinessException(SessionErrorCode.INVALID_SESSION_PERIOD);
        }
        if (!sessionEndAt.isAfter(sessionStartAt)) {
            throw new BusinessException(SessionErrorCode.INVALID_SESSION_SCHEDULE);
        }
        if (!sessionStartAt.isAfter(endAt)) {
            throw new BusinessException(SessionErrorCode.INVALID_SESSION_SCHEDULE_BEFORE_REGISTRATION);
        }
        if (maxHeadcountPerApplication != null && maxHeadcountPerApplication > capacity) {
            throw new BusinessException(SessionErrorCode.MAX_HEADCOUNT_EXCEEDS_CAPACITY);
        }
    }

    private void validateWithinConferencePeriod(LocalDateTime sessionStartAt, LocalDateTime sessionEndAt, Conference conference) {
        if (sessionStartAt.isBefore(conference.getStartAt()) || sessionEndAt.isAfter(conference.getEndAt())) {
            throw new BusinessException(SessionErrorCode.SESSION_SCHEDULE_OUTSIDE_CONFERENCE_PERIOD);
        }
    }

    private void validateCapacityWithinConference(Conference conference, int requestedCapacity) {
        if (requestedCapacity > conference.getCapacity()) {
            throw new BusinessException(SessionErrorCode.SESSION_CAPACITY_EXCEEDS_CONFERENCE_CAPACITY);
        }
    }

    @Transactional
    public SessionResponse approveSession(UUID id) {
        Session session = findSession(id);
        if (!session.isPending()) {
            throw new BusinessException(SessionErrorCode.SESSION_ALREADY_DECIDED);
        }
        session.approve();
        return SessionResponse.from(session);
    }

    @Transactional
    public SessionResponse rejectSession(UUID id, RejectSessionRequest request) {
        Session session = findSession(id);
        if (!session.isPending()) {
            throw new BusinessException(SessionErrorCode.SESSION_ALREADY_DECIDED);
        }
        session.reject(request.reason());
        return SessionResponse.from(session);
    }

    private Session findSession(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));
    }
}
