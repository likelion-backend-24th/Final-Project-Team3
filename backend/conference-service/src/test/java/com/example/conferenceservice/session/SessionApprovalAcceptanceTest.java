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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story 8 인수 조건: 전체관리자가 신청된 세션을 승인·반려할 수 있고,
 * 승인 전 세션은 컨퍼런스 상세의 세션 목록에 노출되지 않으며, ADMIN 권한 없이는 승인·반려를 호출할 수 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionApprovalAcceptanceTest {

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
    void approveSession_thenExposedInConferenceDetail() throws Exception {
        Conference conference = conferenceRepository.save(approvedConference());
        Session pending = sessionRepository.save(pendingSession(conference, "승인 대상 세션"));

        mockMvc.perform(patch("/api/admin/sessions/{id}/approve", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("승인 대상 세션"));

        mockMvc.perform(get("/api/conferences/{id}", conference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessions", hasSize(1)))
                .andExpect(jsonPath("$.data.sessions[0].title").value("승인 대상 세션"));
    }

    @Test
    void rejectSession_thenNotExposedAndReasonSaved() throws Exception {
        Conference conference = conferenceRepository.save(approvedConference());
        Session pending = sessionRepository.save(pendingSession(conference, "반려 대상 세션"));

        mockMvc.perform(patch("/api/admin/sessions/{id}/reject", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "정원 초과"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("반려 대상 세션"));

        mockMvc.perform(get("/api/conferences/{id}", conference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessions", hasSize(0)));

        Session rejected = sessionRepository.findById(pending.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(SessionStatus.REJECTED);
        assertThat(rejected.getRejectReason()).isEqualTo("정원 초과");
    }

    @Test
    void pendingSession_isExcludedFromConferenceDetail() throws Exception {
        Conference conference = conferenceRepository.save(approvedConference());
        sessionRepository.save(pendingSession(conference, "승인 대기 세션"));

        mockMvc.perform(get("/api/conferences/{id}", conference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessions", hasSize(0)));
    }

    @Test
    void approveSession_withoutAdminRole_returnsForbidden() throws Exception {
        Conference conference = conferenceRepository.save(approvedConference());
        Session pending = sessionRepository.save(pendingSession(conference, "권한 검증용 세션"));

        mockMvc.perform(patch("/api/admin/sessions/{id}/approve", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    private Conference approvedConference() {
        return Conference.builder()
                .organizerId(UUID.randomUUID()).organizerName("주최자").title("컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .startAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(2))
                .location("서울")
                .build();
    }

    private Session pendingSession(Conference conference, String title) {
        return Session.builder()
                .conference(conference).title(title).capacity(30)
                .startAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(2))
                .status(SessionStatus.PENDING)
                .build();
    }

    private String adminToken() {
        return token(MemberRole.ADMIN);
    }

    private String organizerToken() {
        return token(MemberRole.ORGANIZER);
    }

    private String token(MemberRole role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }
}
