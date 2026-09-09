package com.example.conferenceservice.session.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.dto.SessionCreateRequest;
import com.example.conferenceservice.session.dto.SessionUpdateRequest;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
import com.example.conferenceservice.session.exception.SessionErrorCode;
import com.example.conferenceservice.session.repository.SessionRepository;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
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
import static org.mockito.Mockito.verify;

/**
 * Task 7-2 검증: 세션이 속한 컨퍼런스를 소유한 주최자만 해당 세션을 등록·수정할 수 있다.
 * 요청자 organizerId와 컨퍼런스 organizerId를 비교하는 OwnerScopeGuard 동작을 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class SessionOwnerScopeTest {

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
    void 다른_주최자의_컨퍼런스에_세션을_등록하면_403으로_거절된다() {
        UUID conferenceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference approved = Conference.builder()
                .id(conferenceId).organizerId(ownerId).title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        SessionCreateRequest request = SessionRequestFixtures.validCreateRequest("세션 A", 10);
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(approved));

        assertThatThrownBy(() -> sessionService.createSession(conferenceId, request, otherOrganizerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_ACCESS_DENIED);
    }

    @Test
    void 다른_주최자의_컨퍼런스에_속한_세션을_수정하면_403으로_거절된다() {
        UUID sessionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference approved = Conference.builder()
                .id(UUID.randomUUID()).organizerId(ownerId).title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        Session existing = Session.builder()
                .id(sessionId).conference(approved).title("세션 A").capacity(10)
                .build();
        SessionUpdateRequest request = SessionRequestFixtures.validUpdateRequest(50);
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> sessionService.updateSession(sessionId, request, otherOrganizerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_ACCESS_DENIED);
    }

    @Test
    void 본인_소유_컨퍼런스에는_세션을_등록할_수_있다() {
        UUID conferenceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Conference approved = Conference.builder()
                .id(conferenceId).organizerId(ownerId).title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        SessionCreateRequest request = SessionRequestFixtures.validCreateRequest("세션 A", 10);
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(approved));
        given(sessionRepository.save(any(Session.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = sessionService.createSession(conferenceId, request, ownerId);

        assertThat(response.title()).isEqualTo("세션 A");
        assertThat(response.capacity()).isEqualTo(10);
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void 다른_주최자의_컨퍼런스_세션_목록을_조회하면_403으로_거절된다() {
        UUID conferenceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference approved = Conference.builder()
                .id(conferenceId).organizerId(ownerId).title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(approved));

        assertThatThrownBy(() -> sessionService.getSessionsByConference(conferenceId, otherOrganizerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_ACCESS_DENIED);
    }

    @Test
    void 존재하지_않는_컨퍼런스의_세션_목록_조회시_404로_거절된다() {
        UUID missingConferenceId = UUID.randomUUID();
        given(conferenceRepository.findById(missingConferenceId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getSessionsByConference(missingConferenceId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_NOT_FOUND);
    }

    @Test
    void 본인_소유_컨퍼런스의_세션_목록은_상태와_무관하게_모두_조회된다() {
        UUID conferenceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Conference approved = Conference.builder()
                .id(conferenceId).organizerId(ownerId).title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        Session pending = Session.builder()
                .id(UUID.randomUUID()).conference(approved).title("승인 대기 세션").capacity(10)
                .status(SessionStatus.PENDING)
                .build();
        Session approvedSession = Session.builder()
                .id(UUID.randomUUID()).conference(approved).title("승인된 세션").capacity(10)
                .status(SessionStatus.APPROVED)
                .build();
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(approved));
        given(sessionRepository.findByConferenceId(conferenceId)).willReturn(List.of(pending, approvedSession));

        List<SessionResponse> sessions = sessionService.getSessionsByConference(conferenceId, ownerId);

        assertThat(sessions).hasSize(2)
                .extracting(SessionResponse::title)
                .containsExactlyInAnyOrder("승인 대기 세션", "승인된 세션");
    }
}
