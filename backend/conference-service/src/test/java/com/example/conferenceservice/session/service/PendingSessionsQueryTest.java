package com.example.conferenceservice.session.service;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
import com.example.conferenceservice.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PendingSessionsQueryTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ConferenceRepository conferenceRepository;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository, conferenceRepository);
    }

    @Test
    void getPendingSessions_onlyQueriesPendingSessions() {
        Conference approved = Conference.builder()
                .id(UUID.randomUUID()).organizerId(UUID.randomUUID()).title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        Session pending = Session.builder()
                .id(UUID.randomUUID()).conference(approved).title("신청된 세션")
                .capacity(30).status(SessionStatus.PENDING)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        given(sessionRepository.findByStatus(SessionStatus.PENDING, pageable))
                .willReturn(new PageImpl<>(List.of(pending)));

        Page<Session> result = sessionService.getPendingSessions(pageable);

        assertThat(result.getContent()).containsExactly(pending);
        verify(sessionRepository).findByStatus(SessionStatus.PENDING, pageable);
        verify(sessionRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getPendingSessions_neverQueriesApprovedStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        given(sessionRepository.findByStatus(SessionStatus.PENDING, pageable))
                .willReturn(new PageImpl<>(List.of()));

        sessionService.getPendingSessions(pageable);

        verify(sessionRepository, never()).findByStatus(SessionStatus.APPROVED, pageable);
    }
}
