package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.dto.ConferenceDetailResponse;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.conference.repository.ConferenceTagRepository;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConferenceVisibilityTest {

    @Mock
    private ConferenceRepository conferenceRepository;

    @Mock
    private ConferenceTagRepository conferenceTagRepository;

    @Mock
    private SessionRepository sessionRepository;

    private ConferenceService conferenceService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        conferenceService = new ConferenceService(conferenceRepository, conferenceTagRepository, sessionRepository);
    }

    @Test
    void listConferences_onlyQueriesApprovedConferences() {
        Conference approved = Conference.builder()
                .id(UUID.randomUUID()).organizerId(UUID.randomUUID()).organizerName("주최자").title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        given(conferenceRepository.findByStatus(ConferenceStatus.APPROVED, pageable))
                .willReturn(new PageImpl<>(List.of(approved)));

        Page<Conference> result = conferenceService.getConferences(pageable);

        assertThat(result.getContent()).containsExactly(approved);
        verify(conferenceRepository).findByStatus(ConferenceStatus.APPROVED, pageable);
        verify(conferenceRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getConference_whenApproved_returnsDetailWithSessions() {
        UUID conferenceId = UUID.randomUUID();
        Conference approved = Conference.builder()
                .id(conferenceId).organizerId(UUID.randomUUID()).organizerName("주최자").title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        Session session = Session.builder()
                .id(UUID.randomUUID()).conference(approved).title("세션 A").capacity(30)
                .build();
        given(conferenceRepository.findByIdAndStatus(conferenceId, ConferenceStatus.APPROVED))
                .willReturn(Optional.of(approved));
        given(sessionRepository.findByConferenceId(conferenceId)).willReturn(List.of(session));
        given(conferenceTagRepository.findByConferenceId(conferenceId)).willReturn(List.of());

        ConferenceDetailResponse result = conferenceService.getConference(conferenceId);

        assertThat(result.title()).isEqualTo("승인된 컨퍼런스");
        assertThat(result.sessions()).hasSize(1);
        assertThat(result.sessions().get(0).title()).isEqualTo("세션 A");
    }

    @Test
    void getConference_whenPendingOrRejectedOrMissing_isNotExposed() {
        UUID missingId = UUID.randomUUID();
        given(conferenceRepository.findByIdAndStatus(missingId, ConferenceStatus.APPROVED))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> conferenceService.getConference(missingId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_NOT_FOUND);
    }
}
