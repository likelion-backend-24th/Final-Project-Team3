package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.auth.CustomUserDetails;
import com.example.conferenceservice.conference.dto.ConferenceDetailResponse;
import com.example.conferenceservice.conference.dto.ConferenceRequest;
import com.example.conferenceservice.conference.dto.ConferenceResponse;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConferenceService {
    private final ConferenceRepository conferenceRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public ConferenceResponse applyConference(CustomUserDetails currentUser, ConferenceRequest request) {
        Conference conference = Conference.builder()
                .organizerId(currentUser.getMemberId())
                .title(request.title())
                .capacity(request.capacity())
                .status(ConferenceStatus.PENDING)
                .build();
        return ConferenceResponse.from(conferenceRepository.save(conference));
    }

    @Transactional(readOnly = true)
    public Page<Conference> getConferences(Pageable pageable) {
        return conferenceRepository.findByStatus(ConferenceStatus.APPROVED, pageable);
    }

    @Transactional(readOnly = true)
    public ConferenceDetailResponse getConference(UUID id) {
        Conference conference = findApprovedConference(id);
        List<Session> sessions = sessionRepository.findByConferenceId(id);
        return ConferenceDetailResponse.from(conference, sessions);
    }

    private Conference findApprovedConference(UUID id) {
        return conferenceRepository.findByIdAndStatus(id, ConferenceStatus.APPROVED)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
    }
}
