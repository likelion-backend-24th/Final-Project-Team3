package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.conference.dto.ConferenceDetailResponseDto;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConferenceService {
    private final ConferenceRepository conferenceRepository;
    private final SessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public Page<Conference> getConferences(Pageable pageable) {
        return conferenceRepository.findByStatus(ConferenceStatus.APPROVED, pageable);
    }

    @Transactional(readOnly = true)
    public ConferenceDetailResponseDto getConference(Long id) {
        Conference conference = conferenceRepository.findByIdAndStatus(id, ConferenceStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conference not found: " + id));

        List<Session> sessions = sessionRepository.findByConferenceId(id);
        return ConferenceDetailResponseDto.from(conference, sessions);
    }
}
