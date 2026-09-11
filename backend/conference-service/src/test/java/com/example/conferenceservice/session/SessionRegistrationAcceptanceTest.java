package com.example.conferenceservice.session;

import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task 7-3: Story 7(#80) 인수 조건이 실제 API 호출(POST /api/conferences/{conferenceId}/sessions)로
 * 재현 가능함을 검증한다. 정상 등록·정원/기간 유효성 실패(400)·소유권 불일치(403)·컨퍼런스 미승인(409)
 * 4개 케이스를 다룬다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionRegistrationAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 승인된_컨퍼런스_소유자가_세션을_등록하면_정원과_기간이_PENDING_상태로_저장된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference approved = conferenceRepository.save(approvedConference(organizerId));

        mockMvc.perform(post("/api/conferences/{conferenceId}/sessions", approved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequestJson("세션 A", 30)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("세션 A"))
                .andExpect(jsonPath("$.data.capacity").value(30));

        List<Session> saved = sessionRepository.findByConferenceId(approved.getId());
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(SessionStatus.PENDING);
    }

    @Test
    void 정원이_0이하면_세션_등록이_400으로_거절된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference approved = conferenceRepository.save(approvedConference(organizerId));

        mockMvc.perform(post("/api/conferences/{conferenceId}/sessions", approved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequestJson("세션 A", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SESSION_CAPACITY"));

        assertThat(sessionRepository.findByConferenceId(approved.getId())).isEmpty();
    }

    @Test
    void 다른_주최자의_컨퍼런스에_세션을_등록하면_403으로_거절된다() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference approved = conferenceRepository.save(approvedConference(ownerId));

        mockMvc.perform(post("/api/conferences/{conferenceId}/sessions", approved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(otherOrganizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequestJson("세션 A", 30)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_ACCESS_DENIED"));

        assertThat(sessionRepository.findByConferenceId(approved.getId())).isEmpty();
    }

    @Test
    void 승인되지_않은_컨퍼런스에는_세션_등록이_409로_거절된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference pending = conferenceRepository.save(pendingConference(organizerId));

        mockMvc.perform(post("/api/conferences/{conferenceId}/sessions", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequestJson("세션 A", 30)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_NOT_APPROVED"));

        assertThat(sessionRepository.findByConferenceId(pending.getId())).isEmpty();
    }

    private Conference approvedConference(UUID organizerId) {
        return conference(organizerId, ConferenceStatus.APPROVED);
    }

    private Conference pendingConference(UUID organizerId) {
        return conference(organizerId, ConferenceStatus.PENDING);
    }

    private Conference conference(UUID organizerId, ConferenceStatus status) {
        return Conference.builder()
                .organizerId(organizerId).organizerName("주최자").title("컨퍼런스")
                .status(status).capacity(100)
                .startAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(2))
                .location("서울")
                .build();
    }

    private String sessionRequestJson(String title, int capacity) {
        LocalDateTime startAt = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime endAt = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime sessionStartAt = LocalDateTime.now().plusDays(5).withNano(0);
        LocalDateTime sessionEndAt = sessionStartAt.plusHours(1);
        return """
                {
                  "title": "%s",
                  "capacity": %d,
                  "startAt": "%s",
                  "endAt": "%s",
                  "sessionStartAt": "%s",
                  "sessionEndAt": "%s",
                  "location": "그랜드홀 A",
                  "speaker": "김연수 CTO",
                  "price": 10000,
                  "maxHeadcountPerApplication": 4
                }
                """.formatted(title, capacity, startAt, endAt, sessionStartAt, sessionEndAt);
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
