package com.example.conferenceservice.conference;

import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
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
 * Story 6 인수 조건: 전체관리자가 신청된 컨퍼런스를 승인·반려할 수 있고,
 * 승인 전 컨퍼런스는 참가자에게 노출되지 않으며, ADMIN 권한 없이는 승인·반려를 호출할 수 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConferenceApprovalAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        conferenceRepository.deleteAll();
    }

    @Test
    void approveConference_thenExposedToParticipants() throws Exception {
        Conference pending = conferenceRepository.save(pendingConference("승인 대상 컨퍼런스"));

        mockMvc.perform(patch("/api/admin/conferences/{id}/approve", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/conferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(pending.getId().toString()));

        mockMvc.perform(get("/api/conferences/{id}", pending.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void rejectConference_thenNotExposedAndReasonSaved() throws Exception {
        Conference pending = conferenceRepository.save(pendingConference("반려 대상 컨퍼런스"));

        mockMvc.perform(patch("/api/admin/conferences/{id}/reject", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "정원 초과"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(get("/api/conferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/conferences/{id}", pending.getId()))
                .andExpect(status().isNotFound());

        Conference rejected = conferenceRepository.findById(pending.getId()).orElseThrow();
        assertThat(rejected.getRejectionReason()).isEqualTo("정원 초과");
    }

    @Test
    void pendingConference_isExcludedFromListAndDetail() throws Exception {
        Conference pending = conferenceRepository.save(pendingConference("승인 대기 컨퍼런스"));

        mockMvc.perform(get("/api/conferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/conferences/{id}", pending.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_NOT_FOUND"));
    }

    @Test
    void approveConference_withoutAdminRole_returnsForbidden() throws Exception {
        Conference pending = conferenceRepository.save(pendingConference("권한 검증용 컨퍼런스"));

        mockMvc.perform(patch("/api/admin/conferences/{id}/approve", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    private Conference pendingConference(String title) {
        return Conference.builder()
                .organizerId(UUID.randomUUID())
                .organizerName("주최자")
                .title(title)
                .status(ConferenceStatus.PENDING)
                .capacity(100)
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(2))
                .location("서울")
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
