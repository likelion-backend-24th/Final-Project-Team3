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
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/conferences/{conferenceId}/sessions 인수 테스트.
 * 주최자가 본인 컨퍼런스의 세션을 승인 상태와 무관하게 전부 조회할 수 있는지,
 * 소유권 불일치(403)·존재하지 않는 컨퍼런스(404)가 올바르게 거절되는지를 다룬다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionListAcceptanceTest {

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
    void 컨퍼런스_소유자는_승인_대기_세션을_포함해_전체_세션_목록을_조회할_수_있다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference approved = conferenceRepository.save(approvedConference(organizerId));
        sessionRepository.save(session(approved, "승인 대기 세션", SessionStatus.PENDING));
        sessionRepository.save(session(approved, "승인된 세션", SessionStatus.APPROVED));

        mockMvc.perform(get("/api/conferences/{conferenceId}/sessions", approved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void 다른_주최자의_컨퍼런스_세션_목록을_조회하면_403으로_거절된다() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference approved = conferenceRepository.save(approvedConference(ownerId));
        sessionRepository.save(session(approved, "세션 A", SessionStatus.PENDING));

        mockMvc.perform(get("/api/conferences/{conferenceId}/sessions", approved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(otherOrganizerId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_ACCESS_DENIED"));
    }

    @Test
    void 존재하지_않는_컨퍼런스의_세션_목록_조회시_404로_거절된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        UUID missingConferenceId = UUID.randomUUID();

        mockMvc.perform(get("/api/conferences/{conferenceId}/sessions", missingConferenceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_NOT_FOUND"));
    }

    private Conference approvedConference(UUID organizerId) {
        return Conference.builder()
                .organizerId(organizerId).organizerName("주최자").title("컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .startAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(2))
                .location("서울")
                .build();
    }

    private Session session(Conference conference, String title, SessionStatus status) {
        return Session.builder()
                .conference(conference).title(title).capacity(10)
                .startAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(2))
                .status(status)
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
