package com.example.conferenceservice.notice;

import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.notice.entity.ConferenceNotice;
import com.example.conferenceservice.notice.repository.ConferenceNoticeRepository;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨퍼런스 공지 CRUD 배선(라우팅·권한·직렬화) 검증. FAQ는 동일 구조라 서비스 유닛 테스트로만 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NoticeAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private ConferenceNoticeRepository noticeRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        noticeRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 주최자가_공지를_등록하면_목록에서_조회된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));

        mockMvc.perform(post("/api/conferences/{conferenceId}/notices", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "사전 등록 안내", "content": "행사 3일 전까지 가능합니다."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("사전 등록 안내"));

        mockMvc.perform(get("/api/conferences/{conferenceId}/notices", conference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void 다른_주최자가_공지를_수정하면_403으로_거절된다() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(ownerId));
        ConferenceNotice notice = noticeRepository.save(notice(conference));

        mockMvc.perform(patch("/api/conferences/{conferenceId}/notices/{noticeId}", conference.getId(), notice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(otherId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "변경 시도", "content": "변경 시도"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOTICE_ACCESS_DENIED"));
    }

    @Test
    void 주최자가_본인_공지를_삭제하면_목록에서_사라진다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId));
        ConferenceNotice notice = noticeRepository.save(notice(conference));

        mockMvc.perform(delete("/api/conferences/{conferenceId}/notices/{noticeId}", conference.getId(), notice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/conferences/{conferenceId}/notices", conference.getId()))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    private Conference conference(UUID organizerId) {
        return Conference.builder()
                .organizerId(organizerId).organizerName("주최자").title("컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .startAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(2))
                .location("서울")
                .build();
    }

    private ConferenceNotice notice(Conference conference) {
        return ConferenceNotice.builder()
                .conference(conference).title("원래 제목").content("원래 내용")
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
