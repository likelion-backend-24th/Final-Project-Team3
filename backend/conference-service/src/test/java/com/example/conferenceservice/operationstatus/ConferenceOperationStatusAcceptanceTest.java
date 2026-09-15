package com.example.conferenceservice.operationstatus;

import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.operationstatus.client.ReservationServiceClient;
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
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 주최자가 GET /api/conferences/{conferenceId}/operation-status 로
 * 본인 컨퍼런스의 세션별 신청·입장 현황을 조회할 수 있는지 검증한다 (Task 16-2).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConferenceOperationStatusAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @MockitoBean
    private ReservationServiceClient reservationServiceClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 본인_소유_컨퍼런스의_세션별_현황을_조회하면_200과_취합된_결과를_받는다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, "본인 컨퍼런스"));
        Session session = sessionRepository.save(session(conference, "1세션"));

        given(reservationServiceClient.getStatusSummary(session.getId()))
                .willReturn(new ReservationServiceClient.SessionStatusSummaryResponse(
                        session.getId(), 1L, 2L, 3L, 4L, 5L));

        mockMvc.perform(get("/api/conferences/{conferenceId}/operation-status", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conferenceId").value(conference.getId().toString()))
                .andExpect(jsonPath("$.data.sessions[0].sessionId").value(session.getId().toString()))
                .andExpect(jsonPath("$.data.sessions[0].holdCount").value(1))
                .andExpect(jsonPath("$.data.sessions[0].queuedCount").value(2))
                .andExpect(jsonPath("$.data.sessions[0].confirmedCount").value(3))
                .andExpect(jsonPath("$.data.sessions[0].cancelledCount").value(4))
                .andExpect(jsonPath("$.data.sessions[0].checkedInCount").value(5));
    }

    @Test
    void 다른_주최자의_컨퍼런스를_조회하면_403이_반환된다() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(ownerId, "타인 컨퍼런스"));

        mockMvc.perform(get("/api/conferences/{conferenceId}/operation-status", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(otherOrganizerId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 인증없이_요청하면_403으로_거절된다() throws Exception {
        mockMvc.perform(get("/api/conferences/{conferenceId}/operation-status", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    private Conference conference(UUID organizerId, String title) {
        return Conference.builder()
                .organizerId(organizerId).organizerName("주최자").title(title)
                .status(ConferenceStatus.APPROVED).capacity(100)
                .startAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(2))
                .location("서울")
                .build();
    }

    private Session session(Conference conference, String title) {
        return Session.builder()
                .conference(conference)
                .title(title)
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
