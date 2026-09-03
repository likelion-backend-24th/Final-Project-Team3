package com.example.conferenceservice.session.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.session.dto.SessionCapacityResponse;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.exception.SessionErrorCode;
import com.example.conferenceservice.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public SessionCapacityResponse getCapacity(UUID sessionId) {
        Session session = sessionRepository.findByIdAndConference_Status(sessionId, ConferenceStatus.APPROVED)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));
        return SessionCapacityResponse.from(session);
    }
}
