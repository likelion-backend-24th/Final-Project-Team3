package com.example.reservationservice.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SessionHoldAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @AfterEach
    void tearDown() {
        waitingQueueRepository.deleteAll();
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
    }

    private RequestPostProcessor asUser(UUID memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, MemberRole.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

    @Test
    @DisplayName("정원 내 신청 시 홀드가 생성되고 201을 반환한다")
    void createHold_success() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        String requestBody = """
                {
                    "sessionId": "%s",
                    "headcount": 1,
                    "attendees": [{"ageGroup": "TWENTIES", "job": "DEVELOPER"}]
                }
                """.formatted(sessionId);

        mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("HOLD"))
                .andExpect(jsonPath("$.data.reservationId").exists());
    }

    @Test
    @DisplayName("정원 초과 시 대기열에 등록되고 409를 반환한다")
    void createHold_queued_whenCapacityExceeded() throws Exception {
        UUID sessionId = UUID.randomUUID();

        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        for (int i = 0; i < 10; i++) {
            String body = """
                    {"sessionId": "%s", "headcount": 1, "attendees": [{"ageGroup": "TWENTIES", "job": "DEVELOPER"}]}
                    """.formatted(sessionId);
            mockMvc.perform(post("/api/reservations/hold")
                    .with(asUser(UUID.randomUUID()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
        }

        String overflowBody = """
                {"sessionId": "%s", "headcount": 1, "attendees": [{"ageGroup": "TWENTIES", "job": "DEVELOPER"}]}
                """.formatted(sessionId);

        mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overflowBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.queuePosition").value(1));
    }
}