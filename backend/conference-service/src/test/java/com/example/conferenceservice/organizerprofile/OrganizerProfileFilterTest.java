package com.example.conferenceservice.organizerprofile;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story 21: PENDING·REJECTED 상태 컨퍼런스는 주최자 프로필 목록에서 제외되는지 검증한다 ([주최자프로필-승인컨퍼런스만]).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrganizerProfileFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @AfterEach
    void tearDown() {
        conferenceRepository.deleteAll();
    }

    @Test
    void PENDING과_REJECTED_컨퍼런스는_목록에서_제외된다() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Conference approved = conferenceRepository.save(conference(organizerId, ConferenceStatus.APPROVED, "승인된 컨퍼런스"));
        conferenceRepository.save(conference(organizerId, ConferenceStatus.PENDING, "심사 중인 컨퍼런스"));
        conferenceRepository.save(conference(organizerId, ConferenceStatus.REJECTED, "반려된 컨퍼런스"));

        mockMvc.perform(get("/api/organizers/{organizerId}/profile", organizerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pastConferences", hasSize(1)))
                .andExpect(jsonPath("$.data.pastConferences[0].conferenceId").value(approved.getId().toString()));
    }

    private Conference conference(UUID organizerId, ConferenceStatus status, String title) {
        return Conference.builder()
                .organizerId(organizerId)
                .organizerName("주최자")
                .title(title)
                .status(status)
                .capacity(100)
                .startAt(LocalDateTime.now().minusDays(10))
                .endAt(LocalDateTime.now().minusDays(9))
                .location("서울")
                .build();
    }
}
