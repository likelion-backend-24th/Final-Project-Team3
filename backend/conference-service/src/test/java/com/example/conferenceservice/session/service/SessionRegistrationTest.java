package com.example.conferenceservice.session.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.dto.SessionCreateRequest;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.dto.SessionUpdateRequest;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
import com.example.conferenceservice.session.exception.SessionErrorCode;
import com.example.conferenceservice.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Task 7-1 검증: 주최자가 승인된 컨퍼런스에 세션을 등록·수정한다.
 * 정원·신청 기간 유효성([세션정원-유효성] 규칙)과 컨퍼런스 승인 상태 검증을 다룬다.
 * 소유권(Owner Scope) 검증은 Task 7-2 책임이라 SessionOwnerScopeTest에서 다룬다.
 * 이 Test의 요청자는 항상 대상 컨퍼런스의 소유자(ORGANIZER_ID)로 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class SessionRegistrationTest {

    private static final UUID ORGANIZER_ID = UUID.randomUUID();

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
    void 승인된_컨퍼런스에_세션을_등록하면_정원과_기간이_저장된다() {
        UUID conferenceId = UUID.randomUUID();
        Conference approved = approvedConference(conferenceId);
        SessionCreateRequest request = new SessionCreateRequest(
                "세션 A", 30, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(approved));
        given(sessionRepository.save(any(Session.class))).willAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = sessionService.createSession(conferenceId, request, ORGANIZER_ID);

        assertThat(response.title()).isEqualTo("세션 A");
        assertThat(response.capacity()).isEqualTo(30);
        assertThat(response.startAt()).isBefore(response.endAt());

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SessionStatus.PENDING);
    }

    @Test
    void 정원이_0이하면_세션_등록이_400으로_거절된다() {
        UUID conferenceId = UUID.randomUUID();
        SessionCreateRequest request = new SessionCreateRequest(
                "세션 A", 0, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> sessionService.createSession(conferenceId, request, ORGANIZER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.INVALID_SESSION_CAPACITY);
    }

    @Test
    void 시작일이_종료일보다_같거나_이후면_세션_등록이_400으로_거절된다() {
        UUID conferenceId = UUID.randomUUID();
        LocalDateTime sameInstant = LocalDateTime.now().plusDays(1);
        SessionCreateRequest request = new SessionCreateRequest("세션 A", 10, sameInstant, sameInstant);

        assertThatThrownBy(() -> sessionService.createSession(conferenceId, request, ORGANIZER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.INVALID_SESSION_PERIOD);
    }

    @Test
    void 승인되지_않은_컨퍼런스에는_세션_등록이_409로_거절된다() {
        UUID conferenceId = UUID.randomUUID();
        Conference pending = Conference.builder()
                .id(conferenceId).organizerId(ORGANIZER_ID).title("검토 대기 컨퍼런스")
                .status(ConferenceStatus.PENDING).capacity(100)
                .build();
        SessionCreateRequest request = new SessionCreateRequest(
                "세션 A", 10, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> sessionService.createSession(conferenceId, request, ORGANIZER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.CONFERENCE_NOT_APPROVED);
    }

    @Test
    void 존재하지_않는_컨퍼런스에_세션_등록시_404로_거절된다() {
        UUID missingConferenceId = UUID.randomUUID();
        SessionCreateRequest request = new SessionCreateRequest(
                "세션 A", 10, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        given(conferenceRepository.findById(missingConferenceId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.createSession(missingConferenceId, request, ORGANIZER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_NOT_FOUND);
    }

    @Test
    void 세션의_정원과_기간을_수정할_수_있다() {
        UUID sessionId = UUID.randomUUID();
        Session existing = Session.builder()
                .id(sessionId).conference(approvedConference(UUID.randomUUID())).title("세션 A").capacity(10)
                .build();
        SessionUpdateRequest request = new SessionUpdateRequest(
                50, LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(4));
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(existing));

        SessionResponse response = sessionService.updateSession(sessionId, request, ORGANIZER_ID);

        assertThat(response.capacity()).isEqualTo(50);
    }

    @Test
    void 정원이_0이하면_세션_수정이_400으로_거절된다() {
        UUID sessionId = UUID.randomUUID();
        Session existing = Session.builder()
                .id(sessionId).conference(approvedConference(UUID.randomUUID())).title("세션 A").capacity(10)
                .build();
        SessionUpdateRequest request = new SessionUpdateRequest(
                0, LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(4));

        assertThatThrownBy(() -> sessionService.updateSession(sessionId, request, ORGANIZER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.INVALID_SESSION_CAPACITY);
    }

    @Test
    void 승인되지_않은_컨퍼런스의_세션_수정은_409로_거절된다() {
        UUID sessionId = UUID.randomUUID();
        Conference pending = Conference.builder()
                .id(UUID.randomUUID()).organizerId(ORGANIZER_ID).title("검토 대기 컨퍼런스")
                .status(ConferenceStatus.PENDING).capacity(100)
                .build();
        Session existing = Session.builder()
                .id(sessionId).conference(pending).title("세션 A").capacity(10)
                .build();
        SessionUpdateRequest request = new SessionUpdateRequest(
                50, LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(4));
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> sessionService.updateSession(sessionId, request, ORGANIZER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.CONFERENCE_NOT_APPROVED);
    }

    @Test
    void 존재하지_않는_세션_수정시_404로_거절된다() {
        UUID missingId = UUID.randomUUID();
        SessionUpdateRequest request = new SessionUpdateRequest(
                50, LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(4));
        given(sessionRepository.findById(missingId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.updateSession(missingId, request, ORGANIZER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);
    }

    private Conference approvedConference(UUID id) {
        return Conference.builder()
                .id(id).organizerId(ORGANIZER_ID).title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
    }
}
