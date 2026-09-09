package com.example.conferenceservice.session.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.dto.SessionCreateRequest;
import com.example.conferenceservice.session.dto.SessionUpdateRequest;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.exception.SessionErrorCode;
import com.example.conferenceservice.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
