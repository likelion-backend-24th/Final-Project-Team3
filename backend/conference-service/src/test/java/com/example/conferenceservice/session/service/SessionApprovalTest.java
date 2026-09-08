package com.example.conferenceservice.session.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.dto.RejectSessionRequest;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SessionApprovalTest {

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
    void approveSession_whenPending_setsStatusApproved() {
        UUID sessionId = UUID.randomUUID();
        Session pending = pendingSession(sessionId, "신청된 세션");
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(pending));

        SessionResponse result = sessionService.approveSession(sessionId);

        assertThat(result.id()).isEqualTo(sessionId);
        assertThat(pending.isPending()).isFalse();
    }

    @Test
    void approveSession_whenAlreadyDecided_throwsBusinessException() {
        UUID sessionId = UUID.randomUUID();
        Session approved = pendingSession(sessionId, "이미 승인된 세션");
        approved.approve();
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(approved));

        assertThatThrownBy(() -> sessionService.approveSession(sessionId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_ALREADY_DECIDED);
    }

    @Test
    void approveSession_whenNotFound_throwsBusinessException() {
        UUID missingId = UUID.randomUUID();
        given(sessionRepository.findById(missingId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.approveSession(missingId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    void rejectSession_whenPending_setsStatusRejectedWithReason() {
        UUID sessionId = UUID.randomUUID();
        Session pending = pendingSession(sessionId, "신청된 세션");
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(pending));

        SessionResponse result = sessionService.rejectSession(sessionId, new RejectSessionRequest("정원 초과"));

        assertThat(result.id()).isEqualTo(sessionId);
        assertThat(pending.getRejectReason()).isEqualTo("정원 초과");
    }

    @Test
    void rejectSession_whenAlreadyDecided_throwsBusinessException() {
        UUID sessionId = UUID.randomUUID();
        Session rejected = pendingSession(sessionId, "이미 반려된 세션");
        rejected.reject("사유");
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(rejected));

        assertThatThrownBy(() -> sessionService.rejectSession(sessionId, new RejectSessionRequest("다른 사유")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_ALREADY_DECIDED);
    }

    private Session pendingSession(UUID id, String title) {
        Conference approved = Conference.builder()
                .id(UUID.randomUUID()).organizerId(UUID.randomUUID()).title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        return Session.builder()
                .id(id).conference(approved).title(title).capacity(30)
                .status(SessionStatus.PENDING)
                .build();
    }
}
