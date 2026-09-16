package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.dto.ConferenceDetailResponse;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.entity.ConferenceTag;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.conference.repository.ConferenceTagRepository;
import com.example.conferenceservice.organizerprofile.service.OrganizerProfileService;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
import com.example.conferenceservice.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminConferenceDetailTest {

    @Mock
    private ConferenceRepository conferenceRepository;

    @Mock
    private ConferenceTagRepository conferenceTagRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private OrganizerProfileService organizerProfileService;

    private ConferenceService conferenceService;

    @BeforeEach
    void setUp() {
        conferenceService = new ConferenceService(conferenceRepository, conferenceTagRepository, sessionRepository, organizerProfileService);
    }

    @Test
    void getConferenceDetailForAdmin_pendingConference_returnsDetailWithAllSessions() {
        UUID conferenceId = UUID.randomUUID();
        Conference pending = conference(conferenceId, ConferenceStatus.PENDING);
        Session approvedSession = Session.builder().conference(pending).status(SessionStatus.APPROVED).title("승인된 세션").build();
        Session pendingSession = Session.builder().conference(pending).status(SessionStatus.PENDING).title("대기 세션").build();
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(pending));
        given(sessionRepository.findByConferenceId(conferenceId)).willReturn(List.of(approvedSession, pendingSession));
        given(conferenceTagRepository.findByConferenceId(conferenceId))
                .willReturn(List.of(ConferenceTag.builder().tag("백엔드").build()));
        given(organizerProfileService.getOrganizerSummary(any(), any()))
                .willReturn(new OrganizerProfileService.OrganizerSummary(0, null));

        ConferenceDetailResponse result = conferenceService.getConferenceDetailForAdmin(conferenceId);

        assertThat(result.status()).isEqualTo(ConferenceStatus.PENDING);
        assertThat(result.sessions()).hasSize(2);
        assertThat(result.tags()).containsExactly("백엔드");
    }

    @Test
    void getConferenceDetailForAdmin_rejectedConference_stillReturnsDetail() {
        UUID conferenceId = UUID.randomUUID();
        Conference rejected = conference(conferenceId, ConferenceStatus.REJECTED);
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(rejected));
        given(sessionRepository.findByConferenceId(conferenceId)).willReturn(List.of());
        given(conferenceTagRepository.findByConferenceId(conferenceId)).willReturn(List.of());
        given(organizerProfileService.getOrganizerSummary(any(), any()))
                .willReturn(new OrganizerProfileService.OrganizerSummary(0, null));

        ConferenceDetailResponse result = conferenceService.getConferenceDetailForAdmin(conferenceId);

        assertThat(result.status()).isEqualTo(ConferenceStatus.REJECTED);
    }

    @Test
    void getConferenceDetailForAdmin_notFound_throwsConferenceNotFound() {
        UUID missingId = UUID.randomUUID();
        given(conferenceRepository.findById(missingId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> conferenceService.getConferenceDetailForAdmin(missingId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_NOT_FOUND);
    }

    private Conference conference(UUID id, ConferenceStatus status) {
        return Conference.builder()
                .id(id).organizerId(UUID.randomUUID()).organizerName("주최자").title("컨퍼런스")
                .status(status).capacity(100).location("서울")
                .build();
    }
}
