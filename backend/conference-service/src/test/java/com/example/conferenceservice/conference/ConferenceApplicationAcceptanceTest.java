package com.example.conferenceservice.conference;

import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.jayway.jsonpath.JsonPath;
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
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 9 인수 조건: 주최자가 컨퍼런스를 등록 신청하면 "신청(PENDING)" 상태로 저장되고,
 * 신청 상태 컨퍼런스는 승인 전까지 목록·상세 조회 어디에서도 노출되지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConferenceApplicationAcceptanceTest {

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
    void applyConference_savesConferenceAsPending() throws Exception {
        mockMvc.perform(post("/api/conferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizerName": "김주최",
                                  "title": "신청된 컨퍼런스",
                                  "capacity": 100,
                                  "startAt": "2026-10-01T10:00:00",
                                  "endAt": "2026-10-01T18:00:00",
                                  "location": "서울"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        List<Conference> saved = conferenceRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(ConferenceStatus.PENDING);
    }

    @Test
    void applyConference_thenNotExposedInListOrDetail() throws Exception {
        String response = mockMvc.perform(post("/api/conferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizerName": "김주최",
                                  "title": "비공개 상태 확인용 컨퍼런스",
                                  "capacity": 50,
                                  "startAt": "2026-11-01T10:00:00",
                                  "endAt": "2026-11-01T18:00:00",
                                  "location": "부산"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID conferenceId = UUID.fromString(JsonPath.read(response, "$.data.id"));

        mockMvc.perform(get("/api/conferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/conferences/{id}", conferenceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_NOT_FOUND"));
    }

    private String organizerToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", MemberRole.ORGANIZER.name())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }
}
