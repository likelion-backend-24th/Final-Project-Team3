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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PATCH /api/conferences/{id}/description, /location 인수 테스트.
 * 소개글은 승인 여부와 무관하게 항상 수정 가능하고, 장소는 승인(APPROVED) 이후 주소(location)만
 * 잠기며 교통편·주차·편의시설은 계속 수정 가능함을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConferenceUpdateAcceptanceTest {

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

        Conference updated = conferenceRepository.findById(conference.getId()).orElseThrow();
        assertThat(updated.getDescription()).isEqualTo("수정된 소개글");
        assertThat(updated.getStatus()).isEqualTo(ConferenceStatus.APPROVED);
    }

    @Test
    void 다른_주최자의_컨퍼런스_소개글을_수정하면_403으로_거절된다() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Conference conference = conferenceRepository.save(conference(ownerId, ConferenceStatus.PENDING));

        mockMvc.perform(patch("/api/conferences/{id}/description", conference.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(otherOrganizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "남의 컨퍼런스 수정 시도"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_ACCESS_DENIED"));
    }

    @Test
    void 존재하지_않는_컨퍼런스_소개글_수정시_404로_거절된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(patch("/api/conferences/{id}/description", missingId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken(organizerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "소개글"}
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
                                  "transportation": "지하철 2호선 해운대역 5번 출구",
                                  "parkingInfo": "지하 주차장 이용 가능",
                                  "amenities": "휠체어 대여 가능"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.location").value("부산 해운대구"))
                .andExpect(jsonPath("$.data.transportation").value("지하철 2호선 해운대역 5번 출구"))
                .andExpect(jsonPath("$.data.parkingInfo").value("지하 주차장 이용 가능"))
                .andExpect(jsonPath("$.data.amenities").value("휠체어 대여 가능"));
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
                                  "transportation": "버스 이용 권장",
                                  "parkingInfo": "주차 공간이 협소하니 대중교통을 이용해주세요.",
                                  "amenities": "수유실 운영"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.location").value("서울"))
                .andExpect(jsonPath("$.data.transportation").value("버스 이용 권장"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        assertThat(conferenceRepository.findById(conference.getId()).orElseThrow().getStatus())
                .isEqualTo(ConferenceStatus.APPROVED);
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
                                  "transportation": "지하철 2호선 해운대역 5번 출구"
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
                                {"location": "남의 컨퍼런스 수정 시도"}
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
                                {"location": "서울"}
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
