package com.example.conferenceservice.session.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.session.dto.SessionCapacityResponse;
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
import static org.mockito.BDDMockito.given;

/**
 * Story 3 계약: Reservation-Service가 예약 전 세션 정원을 조회하는 내부 API.
 * 세션 ID로 조회하며, 승인된 컨퍼런스에 속한 세션이 아니면 명확한 실패를 반환한다.
 * Conference-Service 응답 지연·실패 시 Reservation-Service가 정원 초과를 허용하지 않는지는
 * Reservation-Service 쪽 CapacityContractFailureTest의 책임이다.
 */
@ExtendWith(MockitoExtension.class)
class SessionCapacityTest {

    @Mock
    private SessionRepository sessionRepository;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository);
    }

    @Test
    void getCapacity_whenApproved_returnsSessionCapacity() {
        UUID sessionId = UUID.randomUUID();
        Conference approved = Conference.builder()
                .id(UUID.randomUUID()).organizerId(UUID.randomUUID()).title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        Session session = Session.builder()
                .id(sessionId).conference(approved).title("세션 A").capacity(30)
                .build();
        given(sessionRepository.findByIdAndConference_Status(sessionId, ConferenceStatus.APPROVED))
                .willReturn(Optional.of(session));

        SessionCapacityResponse result = sessionService.getCapacity(sessionId);

        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.capacity()).isEqualTo(30);
    }

    @Test
    void getCapacity_whenNotApprovedOrMissing_failsClosed() {
        UUID missingId = UUID.randomUUID();
        given(sessionRepository.findByIdAndConference_Status(missingId, ConferenceStatus.APPROVED))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getCapacity(missingId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);
    }
}
