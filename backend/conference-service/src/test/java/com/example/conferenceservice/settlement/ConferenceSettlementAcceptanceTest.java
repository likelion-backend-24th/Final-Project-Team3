package com.example.conferenceservice.settlement;

import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
import com.example.conferenceservice.settlement.client.PaymentSummaryClient;
import com.example.conferenceservice.settlement.client.PaymentSummaryUnavailableException;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 주최자가 GET /api/conferences/{conferenceId}/settlement 로
 * 본인 컨퍼런스의 정산 현황을 조회할 수 있는지 검증한다 (Story 17).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConferenceSettlementAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @MockitoBean
    private PaymentSummaryClient paymentSummaryClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 본인_소유_컨퍼런스의_정산을_조회하면_200과_취합된_결과를_받는다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, "본인 컨퍼런스"));
        Session session = sessionRepository.save(session(conference));

        given(paymentSummaryClient.getPaymentSummary(List.of(session.getId())))
                .willReturn(new PaymentSummaryClient.PaymentSummaryResponse(100_000, 20_000, 80_000, 10, 2));

        mockMvc.perform(get("/api/conferences/{conferenceId}/settlement", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conferenceId").value(conference.getId().toString()))
                .andExpect(jsonPath("$.data.conferenceTitle").value("본인 컨퍼런스"))
                .andExpect(jsonPath("$.data.totalRevenue").value(100_000))
                .andExpect(jsonPath("$.data.refundedAmount").value(20_000))
                .andExpect(jsonPath("$.data.netRevenue").value(80_000))
                .andExpect(jsonPath("$.data.confirmedCount").value(10))
                .andExpect(jsonPath("$.data.cancelledCount").value(2));
    }

    @Test
    void 다른_주최자의_컨퍼런스를_조회하면_403이_반환된다() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(ownerId, "타인 컨퍼런스"));

        mockMvc.perform(get("/api/conferences/{conferenceId}/settlement", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(otherOrganizerId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 존재하지_않는_컨퍼런스를_조회하면_404가_반환된다() throws Exception {
        mockMvc.perform(get("/api/conferences/{conferenceId}/settlement", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void 인증없이_요청하면_403으로_거절된다() throws Exception {
        mockMvc.perform(get("/api/conferences/{conferenceId}/settlement", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void reservationService_응답_실패시_503이_반환된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, "본인 컨퍼런스"));
        sessionRepository.save(session(conference));

        given(paymentSummaryClient.getPaymentSummary(anyList()))
                .willThrow(new PaymentSummaryUnavailableException(List.of(), new RuntimeException("timeout")));

        mockMvc.perform(get("/api/conferences/{conferenceId}/settlement", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId)))
                .andExpect(status().isServiceUnavailable());
    }

    private Conference conference(UUID organizerId, String title) {
        return Conference.builder()
                .organizerId(organizerId).organizerName("주최자").title(title)
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
