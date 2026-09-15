package com.example.conferenceservice.operationstatus.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.operationstatus.client.ReservationServiceClient;
import com.example.conferenceservice.operationstatus.client.ReservationServiceUnavailableException;
import com.example.conferenceservice.operationstatus.exception.OperationStatusErrorCode;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * Reservation-Service 장애 시 캐시 없이 즉시 503(Fail-closed)으로 응답하는지 검증한다.
 */
@SpringBootTest
class OperationStatusDependencyFailureTest {

    @Autowired
    private ConferenceOperationStatusService conferenceOperationStatusService;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @MockitoBean
    private ReservationServiceClient reservationServiceClient;

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void reservationService_응답_실패시_즉시_503으로_실패한다() {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));
        Session session = sessionRepository.save(session(conference));

        given(reservationServiceClient.getStatusSummary(session.getId()))
                .willThrow(new ReservationServiceUnavailableException(session.getId(), new RuntimeException("timeout")));

        assertThatThrownBy(() -> conferenceOperationStatusService.getOperationStatus(conference.getId(), organizerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(OperationStatusErrorCode.RESERVATION_SERVICE_UNAVAILABLE);
    }

    @Test
    void reservationService_장애가_반복되어도_매번_즉시_503으로_실패한다() {
        // 캐시 없이 fail-closed로 동작함을 확인: 같은 요청을 여러 번 반복해도
        // 이전 실패가 캐시되어 다른 응답으로 바뀌지 않고 매번 동일하게 실패해야 한다.
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));
        Session session = sessionRepository.save(session(conference));

        given(reservationServiceClient.getStatusSummary(session.getId()))
                .willThrow(new ReservationServiceUnavailableException(session.getId(), new RuntimeException("timeout")));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> conferenceOperationStatusService.getOperationStatus(conference.getId(), organizerId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(OperationStatusErrorCode.RESERVATION_SERVICE_UNAVAILABLE);
        }
    }

    private Conference conference(UUID organizerId) {
        return Conference.builder()
                .organizerId(organizerId).organizerName("주최자").title("컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .startAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(2))
                .location("서울")
                .build();
    }

    private Session session(Conference conference) {
        return Session.builder()
                .conference(conference)
                .title("세션")
                .capacity(50)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusHours(1))
                .sessionStartAt(conference.getStartAt().plusHours(1))
                .sessionEndAt(conference.getStartAt().plusHours(2))
                .build();
    }
}
