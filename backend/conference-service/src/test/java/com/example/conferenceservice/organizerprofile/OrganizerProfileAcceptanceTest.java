package com.example.conferenceservice.organizerprofile;

import com.example.conferenceservice.attendeesummary.entity.ConferenceAttendeeSummary;
import com.example.conferenceservice.attendeesummary.repository.ConferenceAttendeeSummaryRepository;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story 21: 방문자가 주최자 프로필에서 승인 이력 컨퍼런스 목록과 AI 요약을 조회할 수 있는지,
 * 컨퍼런스 상세에도 주최자 요약(지난 컨퍼런스 수·대표 요약)이 함께 노출되는지 검증한다 (Task 21-1, 21-2).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrganizerProfileAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private ConferenceAttendeeSummaryRepository summaryRepository;

    @AfterEach
    void tearDown() {
        summaryRepository.deleteAll();
        conferenceRepository.deleteAll();
    }

    @Test
    void 승인_이력_컨퍼런스_목록과_AI_요약을_함께_반환한다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference withSummary = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED, "2024 컨퍼런스"));
        conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED, "2025 컨퍼런스"));
        summaryRepository.save(ConferenceAttendeeSummary.builder()
                .conferenceId(withSummary.getId())
                .checkedInCount(10)
                .ageGroupDistributionJson("{}")
                .jobDistributionJson("{}")
                .summaryText("참석자들의 만족도가 높았습니다.")
                .generatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/organizers/{organizerId}/profile", organizerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizerId").value(organizerId.toString()))
                .andExpect(jsonPath("$.data.organizerName").value("주최자"))
                .andExpect(jsonPath("$.data.pastConferences.length()").value(2));
    }

    @Test
    void 아직_종료되지_않은_승인_컨퍼런스는_ongoingConferences로_분류된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED, "지난 컨퍼런스"));
        conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED, "예정된 컨퍼런스",
                LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(11)));

        mockMvc.perform(get("/api/organizers/{organizerId}/profile", organizerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pastConferences.length()").value(1))
                .andExpect(jsonPath("$.data.pastConferences[0].title").value("지난 컨퍼런스"))
                .andExpect(jsonPath("$.data.ongoingConferences.length()").value(1))
                .andExpect(jsonPath("$.data.ongoingConferences[0].title").value("예정된 컨퍼런스"));
    }

    @Test
    void 컨퍼런스_상세에_주최자의_지난_컨퍼런스_수와_대표_요약이_함께_노출된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference past = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED, "지난 컨퍼런스"));
        Conference current = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED, "현재 컨퍼런스"));
        summaryRepository.save(ConferenceAttendeeSummary.builder()
                .conferenceId(past.getId())
                .checkedInCount(5)
                .ageGroupDistributionJson("{}")
                .jobDistributionJson("{}")
                .summaryText("좋은 평가를 받았습니다.")
                .generatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/conferences/{id}", current.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizerPastConferenceCount").value(1))
                .andExpect(jsonPath("$.data.organizerRepresentativeSummary").value("좋은 평가를 받았습니다."));
    }

    private Conference conference(UUID organizerId, ConferenceStatus status, String title) {
        return conference(organizerId, status, title, LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(9));
    }

    private Conference conference(UUID organizerId, ConferenceStatus status, String title,
                                   LocalDateTime startAt, LocalDateTime endAt) {
        return Conference.builder()
                .organizerId(organizerId)
                .organizerName("주최자")
                .title(title)
                .status(status)
                .capacity(100)
                .startAt(startAt)
                .endAt(endAt)
                .location("서울")
                .build();
    }
}
