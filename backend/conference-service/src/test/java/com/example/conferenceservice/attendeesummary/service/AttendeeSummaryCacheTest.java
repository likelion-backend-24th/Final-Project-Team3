package com.example.conferenceservice.attendeesummary.service;

import com.example.conferenceservice.attendeesummary.client.AttendeeCheckinStatsClient;
import com.example.conferenceservice.attendeesummary.dto.ConferenceAttendeeSummaryResponse;
import com.example.conferenceservice.attendeesummary.repository.ConferenceAttendeeSummaryRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

/**
 * 이전 조회 이후 체크인 수 변동이 없으면 저장된 요약을 재사용하고(재계산 없음),
 * 변동이 있으면 재계산하는지 검증한다 (Task 15-2).
 */
@SpringBootTest
class AttendeeSummaryCacheTest {

    @Autowired
    private ConferenceAttendeeSummaryService service;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ConferenceAttendeeSummaryRepository summaryRepository;

    @MockitoBean
    private AttendeeCheckinStatsClient attendeeCheckinStatsClient;

    @AfterEach
    void tearDown() {
        summaryRepository.deleteAll();
        sessionRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 체크인_수_변동이_없으면_저장된_요약을_그대로_재사용한다() {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));
        Session session = sessionRepository.save(session(conference));

        given(attendeeCheckinStatsClient.getAttendeeCheckinStats(List.of(session.getId())))
                .willReturn(new AttendeeCheckinStatsClient.AttendeeCheckinStatsResponse(5, Map.of(), Map.of()));

        ConferenceAttendeeSummaryResponse first = service.getAttendeeSummary(conference.getId(), organizerId);
        ConferenceAttendeeSummaryResponse second = service.getAttendeeSummary(conference.getId(), organizerId);

        // DB 왕복 시 타임스탬프가 마이크로초 단위로 반올림될 수 있어(나노초 손실) 그 단위로 truncate해서 비교한다.
        assertThat(second.generatedAt().truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(first.generatedAt().truncatedTo(ChronoUnit.MICROS));
        assertThat(summaryRepository.findByConferenceId(conference.getId())).hasValueSatisfying(
                entity -> assertThat(entity.getCheckedInCount()).isEqualTo(5));
    }

    @Test
    void 체크인_수가_변하면_재계산한다() throws InterruptedException {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));
        Session session = sessionRepository.save(session(conference));

        given(attendeeCheckinStatsClient.getAttendeeCheckinStats(List.of(session.getId())))
                .willReturn(new AttendeeCheckinStatsClient.AttendeeCheckinStatsResponse(5, Map.of(), Map.of()));
        ConferenceAttendeeSummaryResponse first = service.getAttendeeSummary(conference.getId(), organizerId);

        Thread.sleep(5); // generatedAt 갱신 여부를 시간 정밀도 문제 없이 비교하기 위한 최소 지연
        given(attendeeCheckinStatsClient.getAttendeeCheckinStats(List.of(session.getId())))
                .willReturn(new AttendeeCheckinStatsClient.AttendeeCheckinStatsResponse(8, Map.of(), Map.of()));
        ConferenceAttendeeSummaryResponse second = service.getAttendeeSummary(conference.getId(), organizerId);

        assertThat(second.checkedInCount()).isEqualTo(8);
        assertThat(second.generatedAt()).isAfter(first.generatedAt());
    }

    @Test
    void 체크인_0명이면_LLM_호출_없이_고정_문구를_저장한다() {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));
        sessionRepository.save(session(conference));

        given(attendeeCheckinStatsClient.getAttendeeCheckinStats(anyList()))
                .willReturn(new AttendeeCheckinStatsClient.AttendeeCheckinStatsResponse(0, Map.of(), Map.of()));

        ConferenceAttendeeSummaryResponse response = service.getAttendeeSummary(conference.getId(), organizerId);

        assertThat(response.summaryText()).isEqualTo("아직 체크인한 참석자가 없습니다.");
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
