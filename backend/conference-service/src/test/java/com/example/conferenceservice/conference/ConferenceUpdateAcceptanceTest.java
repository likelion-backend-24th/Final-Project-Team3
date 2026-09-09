package com.example.conferenceservice.conference;

import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.entity.ConferenceTag;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.conference.repository.ConferenceTagRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PATCH /api/conferences/{id}(제목·정원·신청기간·소개글·이미지·카테고리), /location 인수 테스트.
 * 소유자만 수정 가능(403)하고, 승인(APPROVED) 이후엔 주소(location)는 잠기지만
 * 교통편·주차·편의시설 안내(locationDetail)는 계속 수정 가능함을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConferenceUpdateAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private ConferenceTagRepository conferenceTagRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        conferenceTagRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 소유자는_제목_정원_기간_소개글_이미지_카테고리를_한번에_수정할_수_있다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.PENDING));
        conferenceTagRepository.save(ConferenceTag.builder().conference(conference).tag("개발").build());

        mockMvc.perform(patch("/api/conferences/{id}", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 컨퍼런스",
                                  "capacity": 200,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-02T18:00:00",
                                  "description": "수정된 소개글",
                                  "imageUrl": "https://example.com/new.png",
                                  "tags": ["디자인", "기획"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 컨퍼런스"))
                .andExpect(jsonPath("$.data.capacity").value(200))
                .andExpect(jsonPath("$.data.description").value("수정된 소개글"));

        Conference updated = conferenceRepository.findById(conference.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정된 컨퍼런스");
        assertThat(updated.getCapacity()).isEqualTo(200);
        assertThat(conferenceTagRepository.findByConferenceId(conference.getId()))
                .extracting(ConferenceTag::getTag)
                .containsExactlyInAnyOrder("디자인", "기획");
    }

    @Test
    void 승인된_컨퍼런스를_수정하면_다시_승인_대기_상태로_바뀐다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED));

        mockMvc.perform(patch("/api/conferences/{id}", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 컨퍼런스",
                                  "capacity": 200,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-02T18:00:00",
                                  "tags": ["개발"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertThat(conferenceRepository.findById(conference.getId()).orElseThrow().getStatus())
                .isEqualTo(ConferenceStatus.PENDING);
    }

    @Test
    void 반려된_컨퍼런스를_수정하면_다시_승인_대기_상태로_바뀌고_반려사유가_지워진다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference rejected = conference(organizerId, ConferenceStatus.REJECTED);
        rejected.reject("정원 초과");
        Conference conference = conferenceRepository.save(rejected);

        mockMvc.perform(patch("/api/conferences/{id}", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 컨퍼런스",
                                  "capacity": 100,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-02T18:00:00",
                                  "tags": ["개발"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        Conference updated = conferenceRepository.findById(conference.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ConferenceStatus.PENDING);
        assertThat(updated.getRejectionReason()).isNull();
    }

    @Test
    void 신청기간_종료일시가_시작일시보다_이전이면_400으로_거절된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.PENDING));

        mockMvc.perform(patch("/api/conferences/{id}", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "컨퍼런스",
                                  "capacity": 100,
                                  "startAt": "2026-12-02T18:00:00",
                                  "endAt": "2026-12-01T10:00:00",
                                  "tags": ["개발"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CONFERENCE_PERIOD"));
    }

    @Test
    void 카테고리가_없으면_400으로_거절된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.PENDING));

        mockMvc.perform(patch("/api/conferences/{id}", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "컨퍼런스",
                                  "capacity": 100,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-02T18:00:00",
                                  "tags": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 다른_주최자의_컨퍼런스를_수정하면_403으로_거절된다() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(ownerId, ConferenceStatus.PENDING));

        mockMvc.perform(patch("/api/conferences/{id}", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(otherOrganizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "남의 컨퍼런스 수정 시도",
                                  "capacity": 100,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-02T18:00:00",
                                  "tags": ["개발"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_ACCESS_DENIED"));
    }

    @Test
    void 승인된_세션이_있는_컨퍼런스를_수정해도_세션_개수는_그대로_보여진다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED));
        sessionRepository.save(session(conference, SessionStatus.APPROVED));
        sessionRepository.save(session(conference, SessionStatus.APPROVED));
        sessionRepository.save(session(conference, SessionStatus.PENDING));

        mockMvc.perform(patch("/api/conferences/{id}", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "컨퍼런스",
                                  "capacity": 100,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-02T18:00:00",
                                  "description": "수정된 소개글",
                                  "tags": ["개발"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionCount").value(2));
    }

    @Test
    void 존재하지_않는_컨퍼런스_수정시_404로_거절된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(patch("/api/conferences/{id}", missingId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "컨퍼런스",
                                  "capacity": 100,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-02T18:00:00",
                                  "tags": ["개발"]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_NOT_FOUND"));
    }

    @Test
    void 승인_전이면_주소와_부가정보를_모두_수정할_수_있다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.PENDING));

        mockMvc.perform(patch("/api/conferences/{id}/location", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "location": "부산 해운대구",
                                  "locationDetail": "지하철 2호선 해운대역 5번 출구"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.location").value("부산 해운대구"))
                .andExpect(jsonPath("$.data.locationDetail").value("지하철 2호선 해운대역 5번 출구"));
    }

    @Test
    void 승인후에는_주소는_그대로_두고_부가정보만_수정할_수_있다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED));

        mockMvc.perform(patch("/api/conferences/{id}/location", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "location": "서울",
                                  "locationDetail": "주차 공간이 협소하니 대중교통을 이용해주세요."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.location").value("서울"))
                .andExpect(jsonPath("$.data.locationDetail").value("주차 공간이 협소하니 대중교통을 이용해주세요."));

        assertThat(conferenceRepository.findById(conference.getId()).orElseThrow().getStatus())
                .isEqualTo(ConferenceStatus.PENDING);
    }

    @Test
    void 승인후_주소를_바꾸려하면_409로_거절되고_변경되지_않는다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED));

        mockMvc.perform(patch("/api/conferences/{id}/location", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "location": "부산 해운대구",
                                  "locationDetail": "지하철 2호선 해운대역 5번 출구"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_LOCATION_ADDRESS_LOCKED"));

        assertThat(conferenceRepository.findById(conference.getId()).orElseThrow().getLocation())
                .isEqualTo("서울");
    }

    @Test
    void 다른_주최자의_컨퍼런스_장소를_수정하면_403으로_거절된다() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(ownerId, ConferenceStatus.PENDING));

        mockMvc.perform(patch("/api/conferences/{id}/location", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(otherOrganizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "location": "남의 컨퍼런스 수정 시도",
                                  "locationDetail": "안내"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_ACCESS_DENIED"));
    }

    @Test
    void 존재하지_않는_컨퍼런스_장소_수정시_404로_거절된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(patch("/api/conferences/{id}/location", missingId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "location": "서울",
                                  "locationDetail": "안내"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_NOT_FOUND"));
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

    private Session session(Conference conference, SessionStatus status) {
        return Session.builder()
                .conference(conference).title("세션").capacity(10)
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
