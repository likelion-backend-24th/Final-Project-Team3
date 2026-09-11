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
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 주최자가 GET /api/conferences/my 로 본인 컨퍼런스를 상태와 무관하게 조회할 수 있는지 검증한다.
 * 공개용 GET /api/conferences(APPROVED만 반환)로는 승인 대기 중인 본인 컨퍼런스가 보이지 않던 문제를 해결한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MyConferencesAcceptanceTest {

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
    void 주최자는_본인의_승인대기_컨퍼런스도_목록에서_조회할_수_있다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        conferenceRepository.save(conference(organizerId, "승인 대기 컨퍼런스", ConferenceStatus.PENDING));
        conferenceRepository.save(conference(organizerId, "승인된 컨퍼런스", ConferenceStatus.APPROVED));

        mockMvc.perform(get("/api/conferences/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void 다른_주최자의_컨퍼런스는_목록에_포함되지_않는다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        conferenceRepository.save(conference(otherOrganizerId, "다른 주최자 컨퍼런스", ConferenceStatus.APPROVED));

        mockMvc.perform(get("/api/conferences/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void 인증없이_요청하면_403으로_거절된다() throws Exception {
        // /api/conferences/** GET은 필터 체인에서 permitAll이라 익명 사용자도 컨트롤러까지 도달하고,
        // @PreAuthorize("hasRole('ORGANIZER')")에서 권한 부족으로 거절되어 403(AccessDenied)이 된다.
        mockMvc.perform(get("/api/conferences/my"))
                .andExpect(status().isForbidden());
    }

    private Conference conference(UUID organizerId, String title, ConferenceStatus status) {
        return Conference.builder()
                .organizerId(organizerId).organizerName("주최자").title(title)
                .status(status).capacity(100)
                .startAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(2))
                .location("서울")
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
