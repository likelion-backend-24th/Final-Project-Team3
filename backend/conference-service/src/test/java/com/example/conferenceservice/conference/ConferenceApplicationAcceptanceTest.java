package com.example.conferenceservice.conference;

import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.conference.repository.ConferenceTagRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @Autowired
    private ConferenceTagRepository conferenceTagRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${app.upload.conference-proof-dir}")
    private String uploadDir;

    @AfterEach
    void tearDown() {
        conferenceTagRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void applyConference_savesConferenceAsPending() throws Exception {
        mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "신청된 컨퍼런스",
                                  "capacity": 100,
                                  "startAt": "2026-10-01T10:00:00",
                                  "endAt": "2026-10-01T18:00:00",
                                  "location": "서울",
                                  "tags": ["개발"]
                                }
                                """))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.proofFileAttached").value(false));

        List<Conference> saved = conferenceRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(ConferenceStatus.PENDING);
    }

    @Test
    void applyConference_organizerNameIsTakenFromJwtOrganizationNameClaim_notRequestBody() throws Exception {
        mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "주최기관명 검증용 컨퍼런스",
                                  "capacity": 100,
                                  "startAt": "2026-10-01T10:00:00",
                                  "endAt": "2026-10-01T18:00:00",
                                  "location": "서울",
                                  "tags": ["개발"]
                                }
                                """))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isCreated());

        List<Conference> saved = conferenceRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getOrganizerName()).isEqualTo("멋쟁이사자처럼");
    }

    @Test
    void applyConference_whenJwtHasNoOrganizationNameClaim_isRejectedWith401() throws Exception {
        mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "주최기관명 없는 토큰",
                                  "capacity": 100,
                                  "startAt": "2026-10-01T10:00:00",
                                  "endAt": "2026-10-01T18:00:00",
                                  "location": "서울",
                                  "tags": ["개발"]
                                }
                                """))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerTokenWithoutOrganizationName()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_NAME_NOT_FOUND"));

        assertThat(conferenceRepository.findAll()).isEmpty();
    }

    @Test
    void applyConference_thenNotExposedInListOrDetail() throws Exception {
        String response = mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "비공개 상태 확인용 컨퍼런스",
                                  "capacity": 50,
                                  "startAt": "2026-11-01T10:00:00",
                                  "endAt": "2026-11-01T18:00:00",
                                  "location": "부산",
                                  "tags": ["개발"]
                                }
                                """))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
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

    @Test
    void applyConference_withProofFile_canBeDownloadedByOwnerButNotByOtherOrganizer() throws Exception {
        String organizerToken = organizerToken();
        MockMultipartFile proofFile = new MockMultipartFile(
                "proofFile", "사업자등록증.pdf", MediaType.APPLICATION_PDF_VALUE,
                "dummy-content".getBytes(StandardCharsets.UTF_8));

        String response = mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "증명 파일 첨부 컨퍼런스",
                                  "capacity": 30,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-01T18:00:00",
                                  "location": "대전",
                                  "tags": ["개발"]
                                }
                                """))
                        .file(proofFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.proofFileAttached").value(true))
                .andReturn().getResponse().getContentAsString();

        UUID conferenceId = UUID.fromString(JsonPath.read(response, "$.data.id"));

        mockMvc.perform(get("/api/conferences/{id}/proof-file", conferenceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes("dummy-content".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/conferences/{id}/proof-file", conferenceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_ACCESS_DENIED"));
    }

    @Test
    void applyConference_withPathTraversalFilename_staysInsideUploadDir() throws Exception {
        MockMultipartFile proofFile = new MockMultipartFile(
                "proofFile", "../../../../etc/cron.d/evil.pdf", MediaType.APPLICATION_PDF_VALUE,
                "dummy-content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "경로 순회 시도 컨퍼런스",
                                  "capacity": 30,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-01T18:00:00",
                                  "location": "대전",
                                  "tags": ["개발"]
                                }
                                """))
                        .file(proofFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.proofFileAttached").value(true));

        List<Conference> saved = conferenceRepository.findAll();
        assertThat(saved).hasSize(1);
        String proofFileName = saved.get(0).getProofFileName();
        assertThat(proofFileName).doesNotContain("..", "/", "\\").endsWith("_evil.pdf");
        assertThat(Path.of(uploadDir, proofFileName)).exists();
    }

    @Test
    void applyConference_withDisallowedFileExtension_isRejectedWith400() throws Exception {
        MockMultipartFile proofFile = new MockMultipartFile(
                "proofFile", "malware.exe", "application/octet-stream",
                "dummy-content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "잘못된 파일 형식 컨퍼런스",
                                  "capacity": 30,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-01T18:00:00",
                                  "location": "대전",
                                  "tags": ["개발"]
                                }
                                """))
                        .file(proofFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PROOF_FILE_INVALID_TYPE"));

        assertThat(conferenceRepository.findAll()).isEmpty();
    }

    private String organizerToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", MemberRole.ORGANIZER.name())
                .claim("organizationName", "멋쟁이사자처럼")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    private String organizerTokenWithoutOrganizationName() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", MemberRole.ORGANIZER.name())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    // multipart/form-data 요청의 "request" JSON part - 컨트롤러가 @RequestPart("request")로 받는다.
    private MockMultipartFile requestPart(String json) {
        return new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
    }
}
