package com.example.reservationservice.controller;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType; // 수정됨
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WaitingQueueTest { 

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;

    @BeforeEach
    void setUp() {
        waitingQueueRepository.deleteAll();
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
    }

    @Test
    @DisplayName("정원 초과 시 대기열에 순서대로 등록되고, 순번 조회가 정상 작동한다.")
    void queueRegistrationAndPositionCheckTest() throws Exception {

        UUID sessionId = UUID.randomUUID();

        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        String request1 = createRequestJson(sessionId, UUID.randomUUID());
        mockMvc.perform(post("/api/reservations/hold") // URL 수정 (reservations)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("HOLD"));

        String request2 = createRequestJson(sessionId, UUID.randomUUID());
        MvcResult queuedResult1 = mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request2)) // 괄호 위치 수정
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.queuePosition").value(1))
                .andReturn();

        String request3 = createRequestJson(sessionId, UUID.randomUUID());
        mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.queuePosition").value(2));

        String responseBody = queuedResult1.getResponse().getContentAsString();
        String queuedReservationId = JsonPath.read(responseBody, "$.data.reservationId");

        mockMvc.perform(get("/api/reservations/" + queuedReservationId + "/queue-position"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    private String createRequestJson(UUID sessionId, UUID memberId) {
        return """
                {
                    "sessionId": "%s",
                    "memberId": "%s",
                    "headcount": 1
                }
                """.formatted(sessionId, memberId);
    }
}