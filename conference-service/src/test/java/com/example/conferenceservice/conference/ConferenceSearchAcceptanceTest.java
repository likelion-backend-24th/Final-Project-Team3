package com.example.conferenceservice.conference;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story 1 인수 조건: 방문자는 승인(APPROVED)된 컨퍼런스만 검색·조회할 수 있고,
 * 신청(PENDING)·반려(REJECTED) 상태의 컨퍼런스는 목록과 상세 조회 어디에서도 노출되지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConferenceSearchAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @AfterEach
    void tearDown() {
        conferenceRepository.deleteAll();
    }

    @Test
    void listConferences_exposesOnlyApprovedConferences() throws Exception {
        Conference approved = conferenceRepository.save(conference(ConferenceStatus.APPROVED, "승인된 컨퍼런스"));
        conferenceRepository.save(conference(ConferenceStatus.PENDING, "신청 중인 컨퍼런스"));
        conferenceRepository.save(conference(ConferenceStatus.REJECTED, "반려된 컨퍼런스"));

        mockMvc.perform(get("/api/conferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(approved.getId().toString()))
                .andExpect(jsonPath("$.data[0].title").value("승인된 컨퍼런스"));
    }

    @Test
    void getConference_whenApproved_returnsDetail() throws Exception {
        Conference approved = conferenceRepository.save(conference(ConferenceStatus.APPROVED, "승인된 컨퍼런스"));

        mockMvc.perform(get("/api/conferences/{id}", approved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(approved.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void getConference_whenPendingOrRejectedOrMissing_returnsNotFound() throws Exception {
        Conference pending = conferenceRepository.save(conference(ConferenceStatus.PENDING, "신청 중인 컨퍼런스"));
        Conference rejected = conferenceRepository.save(conference(ConferenceStatus.REJECTED, "반려된 컨퍼런스"));

        mockMvc.perform(get("/api/conferences/{id}", pending.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_NOT_FOUND"));

        mockMvc.perform(get("/api/conferences/{id}", rejected.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_NOT_FOUND"));

        mockMvc.perform(get("/api/conferences/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONFERENCE_NOT_FOUND"));
    }

    private Conference conference(ConferenceStatus status, String title) {
        return Conference.builder()
                .organizerId(UUID.randomUUID())
                .title(title)
                .status(status)
                .capacity(100)
                .build();
    }
}
