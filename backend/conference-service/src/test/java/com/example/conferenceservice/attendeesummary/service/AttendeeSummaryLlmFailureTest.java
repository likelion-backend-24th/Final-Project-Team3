package com.example.conferenceservice.attendeesummary.service;

import com.example.conferenceservice.attendeesummary.client.AttendeeCheckinStatsClient;
import com.example.conferenceservice.attendeesummary.client.AttendeeSummaryLlmClient;
import com.example.conferenceservice.attendeesummary.client.AttendeeSummaryLlmException;
import com.example.conferenceservice.attendeesummary.client.ReviewListClient;
import com.example.conferenceservice.attendeesummary.dto.ConferenceAttendeeSummaryResponse;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

/**
 * Task 15-3: LLM 호출이 실패해도 요청 자체는 실패시키지 않고,
 * summaryText만 고정 문구로 대체한 채 나머지 통계는 정상 반환하는지 검증한다.
 */
@SpringBootTest
class AttendeeSummaryLlmFailureTest {

    @Autowired
    private ConferenceAttendeeSummaryService service;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @MockitoBean
    private AttendeeCheckinStatsClient attendeeCheckinStatsClient;

    @MockitoBean
    private ReviewListClient reviewListClient;

    @MockitoBean
    private AttendeeSummaryLlmClient attendeeSummaryLlmClient;

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void LLM_호출이_실패해도_통계는_정상_반환되고_summaryText만_고정_문구로_대체된다() {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));
        Session session = sessionRepository.save(session(conference));

        given(attendeeCheckinStatsClient.getAttendeeCheckinStats(List.of(session.getId())))
                .willReturn(new AttendeeCheckinStatsClient.AttendeeCheckinStatsResponse(
                        3, Map.of("TWENTIES", 2L), Map.of("DEVELOPER", 3L)));
        given(reviewListClient.getReviews(anyList())).willReturn(List.of());
        given(attendeeSummaryLlmClient.generateSummary(any(), any(), any()))
                .willThrow(new AttendeeSummaryLlmException("Gemini API 호출 실패", new RuntimeException("timeout")));

        ConferenceAttendeeSummaryResponse response = service.getAttendeeSummary(conference.getId(), organizerId);

        assertThat(response.checkedInCount()).isEqualTo(3);
        assertThat(response.ageGroupDistribution().get("TWENTIES")).isEqualTo(2L);
        assertThat(response.summaryText()).isEqualTo("요약 정보를 일시적으로 생성하지 못했습니다.");
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
