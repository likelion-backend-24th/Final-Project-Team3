package com.example.conferenceservice.conference;

import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.faq.entity.ConferenceFaq;
import com.example.conferenceservice.faq.repository.ConferenceFaqRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task 5-7 — Story 5에 추가된 인수조건(소개글/장소/공지/FAQ)을 통합 검증한다.
 * 공지(Notice)는 NoticeAcceptanceTest에서 이미 검증하므로 여기서는 소개글/장소/FAQ만 다룬다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConferenceSettingAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private ConferenceFaqRepository faqRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        faqRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 승인된_컨퍼런스도_소개글은_수정할_수_있다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED));

        mockMvc.perform(patch("/api/conferences/{id}/description", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "수정된 소개글"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("수정된 소개글"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void 승인_전이면_장소_전체를_수정할_수_있다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.PENDING));

        mockMvc.perform(patch("/api/conferences/{id}/location", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "location": "부산 해운대구",
                                  "transportation": "지하철 2호선 해운대역 5번 출구",
                                  "parkingInfo": "지하 주차장 이용 가능",
                                  "amenities": "휠체어 대여 가능"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.location").value("부산 해운대구"));
    }

    @Test
    void 승인후_주소변경은_409이지만_나머지_필드는_수정된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED));

        mockMvc.perform(patch("/api/conferences/{id}/location", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"location": "부산 해운대구"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_LOCATION_ADDRESS_LOCKED"));

        mockMvc.perform(patch("/api/conferences/{id}/location", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"location": "서울", "transportation": "버스 이용 권장"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transportation").value("버스 이용 권장"));
    }

    @Test
    void 주최자가_FAQ를_등록하면_목록에서_조회된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED));

        mockMvc.perform(post("/api/conferences/{conferenceId}/faqs", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "주차가 가능한가요?", "answer": "지하 주차장을 이용해주세요."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.question").value("주차가 가능한가요?"));

        mockMvc.perform(get("/api/conferences/{conferenceId}/faqs", conference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void 다른_주최자가_FAQ를_수정하면_403으로_거절된다() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(ownerId, ConferenceStatus.APPROVED));
        ConferenceFaq faq = faqRepository.save(faq(conference));

        mockMvc.perform(patch("/api/conferences/{conferenceId}/faqs/{faqId}", conference.getId(), faq.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(otherId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "변경 시도", "answer": "변경 시도"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FAQ_ACCESS_DENIED"));
    }

    private Conference conference(UUID organizerId, ConferenceStatus status) {
        return Conference.builder()
                .organizerId(organizerId)
                .organizerName("주최자")
                .title("컨퍼런스")
                .status(status)
                .capacity(100)
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(2))
                .location("서울")
                .build();
    }

    private ConferenceFaq faq(Conference conference) {
        return ConferenceFaq.builder()
                .conference(conference).question("원래 질문").answer("원래 답변")
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
