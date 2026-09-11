package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.conference.dto.ConferenceResponse;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.conference.repository.ConferenceTagRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 주최자가 본인 컨퍼런스를 상태와 무관하게(PENDING·APPROVED·REJECTED) 조회하는 API를 검증한다.
 * 공개용 getConferences(APPROVED만 반환)와 달리 organizerId로만 필터링한다.
 */
@ExtendWith(MockitoExtension.class)
class MyConferencesQueryTest {

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
    void getMyConferences_returnsOrganizerConferencesRegardlessOfStatus() {
        UUID organizerId = UUID.randomUUID();
        Conference pending = Conference.builder()
                .id(UUID.randomUUID()).organizerId(organizerId).organizerName("주최자").title("승인 대기 컨퍼런스")
                .status(ConferenceStatus.PENDING).capacity(100)
                .build();
        Conference approved = Conference.builder()
                .id(UUID.randomUUID()).organizerId(organizerId).organizerName("주최자").title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        given(conferenceRepository.findByOrganizerId(organizerId, pageable))
                .willReturn(new PageImpl<>(List.of(pending, approved)));

        Page<ConferenceResponse> result = conferenceService.getMyConferences(organizerId, pageable);

        assertThat(result.getContent())
                .extracting(ConferenceResponse::title)
                .containsExactlyInAnyOrder("승인 대기 컨퍼런스", "승인된 컨퍼런스");
        verify(conferenceRepository).findByOrganizerId(organizerId, pageable);
        verify(conferenceRepository, never()).findByStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getMyConferences_카운트는_승인대기_세션도_포함한_전체_세션수다() {
        UUID organizerId = UUID.randomUUID();
        UUID conferenceId = UUID.randomUUID();
        Conference approved = Conference.builder()
                .id(conferenceId).organizerId(organizerId).organizerName("주최자").title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        given(conferenceRepository.findByOrganizerId(organizerId, pageable))
                .willReturn(new PageImpl<>(List.of(approved)));
        given(sessionRepository.countByConferenceIdIn(List.of(conferenceId)))
                .willReturn(List.of(sessionCount(conferenceId, 3L)));

        Page<ConferenceResponse> result = conferenceService.getMyConferences(organizerId, pageable);

        assertThat(result.getContent().get(0).sessionCount()).isEqualTo(3L);
        verify(sessionRepository, never()).countByConferenceIdInAndStatus(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(SessionStatus.APPROVED));
    }

    private SessionRepository.ConferenceSessionCount sessionCount(UUID conferenceId, long count) {
        return new SessionRepository.ConferenceSessionCount() {
            @Override
            public UUID getConferenceId() {
                return conferenceId;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    @Test
    void getMyConferences_onlyQueriesGivenOrganizerId() {
        UUID organizerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        given(conferenceRepository.findByOrganizerId(organizerId, pageable))
                .willReturn(new PageImpl<>(List.of()));

        conferenceService.getMyConferences(organizerId, pageable);

        verify(conferenceRepository, never()).findByOrganizerId(org.mockito.ArgumentMatchers.eq(otherOrganizerId), org.mockito.ArgumentMatchers.any());
    }
}
