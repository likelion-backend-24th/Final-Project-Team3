package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.conference.repository.ConferenceTagRepository;
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
class PendingConferencesQueryTest {

    @Mock
    private ConferenceRepository conferenceRepository;

    @Mock
    private ConferenceTagRepository conferenceTagRepository;

    @Mock
    private SessionRepository sessionRepository;

    private ConferenceService conferenceService;

    @BeforeEach
    void setUp() {
        conferenceService = new ConferenceService(conferenceRepository, conferenceTagRepository, sessionRepository);
    }

    @Test
    void getPendingConferences_onlyQueriesPendingConferences() {
        Conference pending = Conference.builder()
                .id(UUID.randomUUID()).organizerId(UUID.randomUUID()).organizerName("주최자").title("신청된 컨퍼런스")
                .status(ConferenceStatus.PENDING).capacity(100)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        given(conferenceRepository.findByStatus(ConferenceStatus.PENDING, pageable))
                .willReturn(new PageImpl<>(List.of(pending)));

        Page<Conference> result = conferenceService.getPendingConferences(pageable);

        assertThat(result.getContent()).containsExactly(pending);
        verify(conferenceRepository).findByStatus(ConferenceStatus.PENDING, pageable);
        verify(conferenceRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getPendingConferences_neverQueriesApprovedStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        given(conferenceRepository.findByStatus(ConferenceStatus.PENDING, pageable))
                .willReturn(new PageImpl<>(List.of()));

        conferenceService.getPendingConferences(pageable);

        verify(conferenceRepository, never()).findByStatus(ConferenceStatus.APPROVED, pageable);
    }
}
