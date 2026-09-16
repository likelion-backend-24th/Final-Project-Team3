package com.example.conferenceservice.attendeesummary;

import com.example.conferenceservice.attendeesummary.client.AttendeeCheckinStatsClient;
import com.example.conferenceservice.attendeesummary.client.AttendeeSummaryLlmClient;
import com.example.conferenceservice.attendeesummary.client.ReviewListClient;
import com.example.conferenceservice.attendeesummary.repository.ConferenceAttendeeSummaryRepository;
import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 주최자가 GET /api/conferences/{conferenceId}/attendee-summary 로
 * 본인 컨퍼런스의 체크인 완료 참석자 통계를 조회할 수 있는지 검증한다 (Task 15-2).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AttendeeSummaryAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ConferenceAttendeeSummaryRepository summaryRepository;

    @MockitoBean
    private AttendeeCheckinStatsClient attendeeCheckinStatsClient;

    @MockitoBean
    private ReviewListClient reviewListClient;

    @MockitoBean
    private AttendeeSummaryLlmClient attendeeSummaryLlmClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        summaryRepository.deleteAll();
        sessionRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 본인_소유_컨퍼런스의_참석자_통계를_조회하면_200과_통계를_받는다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));
        Session session = sessionRepository.save(session(conference));

        given(attendeeCheckinStatsClient.getAttendeeCheckinStats(List.of(session.getId())))
                .willReturn(new AttendeeCheckinStatsClient.AttendeeCheckinStatsResponse(
                        3, Map.of("TWENTIES", 2L, "THIRTIES", 1L), Map.of("DEVELOPER", 3L)));
        given(reviewListClient.getReviews(anyList())).willReturn(List.of());
        given(attendeeSummaryLlmClient.generateSummary(any(), any(), any())).willReturn("생성된 요약입니다");

        mockMvc.perform(get("/api/conferences/{conferenceId}/attendee-summary", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conferenceId").value(conference.getId().toString()))
                .andExpect(jsonPath("$.data.checkedInCount").value(3))
                .andExpect(jsonPath("$.data.ageGroupDistribution.TWENTIES").value(2))
                .andExpect(jsonPath("$.data.jobDistribution.DEVELOPER").value(3))
                .andExpect(jsonPath("$.data.summaryText").value("생성된 요약입니다"));
    }

    @Test
    void 체크인_0명이면_고정_문구를_반환한다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));
        sessionRepository.save(session(conference));

        given(attendeeCheckinStatsClient.getAttendeeCheckinStats(anyList()))
                .willReturn(new AttendeeCheckinStatsClient.AttendeeCheckinStatsResponse(0, Map.of(), Map.of()));

        mockMvc.perform(get("/api/conferences/{conferenceId}/attendee-summary", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedInCount").value(0))
                .andExpect(jsonPath("$.data.summaryText").value("아직 체크인한 참석자가 없습니다."));
    }

    @Test
    void 다른_주최자의_컨퍼런스를_조회하면_403이_반환된다() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(ownerId));

        mockMvc.perform(get("/api/conferences/{conferenceId}/attendee-summary", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(otherOrganizerId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 인증없이_요청하면_403으로_거절된다() throws Exception {
        mockMvc.perform(get("/api/conferences/{conferenceId}/attendee-summary", UUID.randomUUID()))
                .andExpect(status().isForbidden());
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

    private String organizerToken(UUID memberId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(memberId.toString())
                .claim("role", MemberRole.ORGANIZER.name())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }
}
