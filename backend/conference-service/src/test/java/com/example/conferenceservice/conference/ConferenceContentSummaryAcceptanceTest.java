package com.example.conferenceservice.conference;

import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.conference.client.ConferenceContentSummaryLlmClient;
import com.example.conferenceservice.conference.client.ConferenceContentSummaryLlmException;
import com.example.conferenceservice.conference.entity.Conference;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 소개글을 저장(등록/수정)하는 시점에 AI 요약이 생성·갱신되고, LLM 실패가 저장 자체를 막지 않으며,
 * 소개글 이미지가 2장 이상이면 저장이 거부되는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConferenceContentSummaryAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private ConferenceTagRepository conferenceTagRepository;

    @MockitoBean
    private ConferenceContentSummaryLlmClient llmClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        conferenceTagRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 컨퍼런스_등록_시점에_AI_요약이_생성된다() throws Exception {
        given(llmClient.generateSummary(any(), any())).willReturn("테스트 요약");

        String response = mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "AI 요약 대상 컨퍼런스",
                                  "capacity": 30,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-01T18:00:00",
                                  "location": "대전",
                                  "description": "개발자를 위한 컨퍼런스입니다.",
                                  "tags": ["개발"]
                                }
                                """))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.aiSummary").value("테스트 요약"))
                .andReturn().getResponse().getContentAsString();

        UUID conferenceId = UUID.fromString(JsonPath.read(response, "$.data.id"));
        Conference saved = conferenceRepository.findById(conferenceId).orElseThrow();
        assertThat(saved.getAiSummary()).isEqualTo("테스트 요약");
    }

    @Test
    void 소개글_수정_시점에_AI_요약이_다시_생성된다() throws Exception {
        String organizerToken = organizerToken();
        given(llmClient.generateSummary(any(), any())).willReturn("최초 요약");

        String response = mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "소개글 수정 대상 컨퍼런스",
                                  "capacity": 30,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-01T18:00:00",
                                  "location": "대전",
                                  "description": "최초 소개글",
                                  "tags": ["개발"]
                                }
                                """))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID conferenceId = UUID.fromString(JsonPath.read(response, "$.data.id"));

        given(llmClient.generateSummary(any(), any())).willReturn("갱신된 요약");

        mockMvc.perform(patch("/api/conferences/{id}/description", conferenceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "새로 고친 소개글"}
                                """)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiSummary").value("갱신된 요약"));

        assertThat(conferenceRepository.findById(conferenceId).orElseThrow().getAiSummary())
                .isEqualTo("갱신된 요약");
    }

    @Test
    void LLM_호출이_실패해도_컨퍼런스_등록은_성공하고_aiSummary는_비워둔다() throws Exception {
        given(llmClient.generateSummary(any(), any()))
                .willThrow(new ConferenceContentSummaryLlmException("Gemini API 호출 실패", new RuntimeException("timeout")));

        String response = mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "LLM 실패 컨퍼런스",
                                  "capacity": 30,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-01T18:00:00",
                                  "location": "대전",
                                  "description": "개발자를 위한 컨퍼런스입니다.",
                                  "tags": ["개발"]
                                }
                                """))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.aiSummary").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        UUID conferenceId = UUID.fromString(JsonPath.read(response, "$.data.id"));
        assertThat(conferenceRepository.findById(conferenceId).orElseThrow().getAiSummary()).isNull();
    }

    @Test
    void 소개글에_이미지가_2장_이상이면_등록이_거부된다() throws Exception {
        mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "이미지 초과 컨퍼런스",
                                  "capacity": 30,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-01T18:00:00",
                                  "location": "대전",
                                  "description": "소개 ![a](/api/conferences/images/desc_1.jpg) ![b](/api/conferences/images/desc_2.jpg)",
                                  "tags": ["개발"]
                                }
                                """))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DESCRIPTION_IMAGE_LIMIT_EXCEEDED"));

        assertThat(conferenceRepository.findAll()).isEmpty();
    }

    @Test
    void 소개글_수정_시에도_이미지가_2장_이상이면_거부된다() throws Exception {
        String organizerToken = organizerToken();
        given(llmClient.generateSummary(any(), any())).willReturn("최초 요약");

        String response = mockMvc.perform(multipart("/api/conferences")
                        .file(requestPart("""
                                {
                                  "title": "소개글 수정 이미지 초과 컨퍼런스",
                                  "capacity": 30,
                                  "startAt": "2026-12-01T10:00:00",
                                  "endAt": "2026-12-01T18:00:00",
                                  "location": "대전",
                                  "description": "최초 소개글",
                                  "tags": ["개발"]
                                }
                                """))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID conferenceId = UUID.fromString(JsonPath.read(response, "$.data.id"));

        mockMvc.perform(patch("/api/conferences/{id}/description", conferenceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "수정 ![a](/api/conferences/images/desc_1.jpg) ![b](/api/conferences/images/desc_2.jpg)"}
                                """)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DESCRIPTION_IMAGE_LIMIT_EXCEEDED"));

        assertThat(conferenceRepository.findById(conferenceId).orElseThrow().getDescription())
                .isEqualTo("최초 소개글");
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

    private MockMultipartFile requestPart(String json) {
        return new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
    }
}
