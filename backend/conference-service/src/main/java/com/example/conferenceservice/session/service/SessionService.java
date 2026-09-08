package com.example.conferenceservice.session.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.dto.SessionCapacityResponse;
import com.example.conferenceservice.session.dto.SessionCreateRequest;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.dto.SessionUpdateRequest;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.exception.SessionErrorCode;
import com.example.conferenceservice.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final ConferenceRepository conferenceRepository;

    @Transactional(readOnly = true)
    public SessionCapacityResponse getCapacity(UUID sessionId) {
        Session session = sessionRepository.findByIdAndConference_Status(sessionId, ConferenceStatus.APPROVED)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));
        return SessionCapacityResponse.from(session);
    }

    @Transactional
    public SessionResponse createSession(UUID conferenceId, SessionCreateRequest request) {
        validatePeriod(request.startAt(), request.endAt());

        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
        if (conference.getStatus() != ConferenceStatus.APPROVED) {
            throw new BusinessException(SessionErrorCode.CONFERENCE_NOT_APPROVED);
        }

        Session session = Session.builder()
                .conference(conference)
                .title(request.title())
                .capacity(request.capacity())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .build();
        Session saved = sessionRepository.save(session);
        return SessionResponse.from(saved);
    }

    @Transactional
    public SessionResponse updateSession(UUID sessionId, SessionUpdateRequest request) {
        validatePeriod(request.startAt(), request.endAt());

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));
        if (session.getConference().getStatus() != ConferenceStatus.APPROVED) {
            throw new BusinessException(SessionErrorCode.CONFERENCE_NOT_APPROVED);
        }
        session.updateSchedule(request.capacity(), request.startAt(), request.endAt());
        return SessionResponse.from(session);
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new BusinessException(SessionErrorCode.INVALID_SESSION_PERIOD);
        }
    }
}
