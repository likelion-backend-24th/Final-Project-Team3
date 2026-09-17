package com.example.conferenceservice.organizerprofile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story 21: 승인 이력이 없는 주최자를 조회해도 에러 없이 빈 목록으로 응답하는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrganizerProfileEmptyHistoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 승인_이력이_없으면_빈_목록으로_응답한다() throws Exception {
        UUID unknownOrganizerId = UUID.randomUUID();

        mockMvc.perform(get("/api/organizers/{organizerId}/profile", unknownOrganizerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizerId").value(unknownOrganizerId.toString()))
                .andExpect(jsonPath("$.data.pastConferences", hasSize(0)));
    }
}
