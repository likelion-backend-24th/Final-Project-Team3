package com.example.conferenceservice.attendeesummary.service;

import com.example.conferenceservice.attendeesummary.client.AttendeeCheckinStatsClient;
import com.example.conferenceservice.attendeesummary.client.AttendeeStatsUnavailableException;
import com.example.conferenceservice.attendeesummary.exception.AttendeeSummaryErrorCode;
import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * Reservation-Service의 체크인 집계 API가 실패하면 캐시 없이 즉시 503(Fail-closed)으로 응답하는지 검증한다 (Task 15-2 계약).
 */
@SpringBootTest
class AttendeeCheckinStatsContractFailureTest {

    @Autowired
    private ConferenceAttendeeSummaryService service;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @MockitoBean
    private AttendeeCheckinStatsClient attendeeCheckinStatsClient;

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 체크인_통계_조회가_실패하면_즉시_503으로_실패한다() {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));
        Session session = sessionRepository.save(session(conference));

        given(attendeeCheckinStatsClient.getAttendeeCheckinStats(List.of(session.getId())))
                .willThrow(new AttendeeStatsUnavailableException(List.of(session.getId()), new RuntimeException("timeout")));

        assertThatThrownBy(() -> service.getAttendeeSummary(conference.getId(), organizerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AttendeeSummaryErrorCode.RESERVATION_SERVICE_UNAVAILABLE);
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
