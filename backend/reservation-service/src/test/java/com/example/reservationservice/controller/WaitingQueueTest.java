package com.example.reservationservice.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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

    private RequestPostProcessor asUser(UUID memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, MemberRole.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

    @Test
    @DisplayName("정원 초과 시 대기열에 순서대로 등록되고, 순번 조회가 정상 작동한다.")
    void queueRegistrationAndPositionCheckTest() throws Exception {

        UUID sessionId = UUID.randomUUID();
        UUID queuedMemberId = UUID.randomUUID();

        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        String request1 = createRequestJson(sessionId);
        mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("HOLD"));

        String request2 = createRequestJson(sessionId);
        MvcResult queuedResult1 = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(queuedMemberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.queuePosition").value(1))
                .andReturn();

        String request3 = createRequestJson(sessionId);
        mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.queuePosition").value(2));

        String responseBody = queuedResult1.getResponse().getContentAsString();
        String queuedReservationId = JsonPath.read(responseBody, "$.data.reservationId");

        mockMvc.perform(get("/api/reservations/" + queuedReservationId + "/queue-position")
                        .with(asUser(queuedMemberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.position").value(1));
    }

    private String createRequestJson(UUID sessionId) {
        return """
            {
                "sessionId": "%s",
                "headcount": 1,
                "attendees": [{"ageGroup": "TWENTIES", "job": "DEVELOPER"}]
            }
            """.formatted(sessionId);
    }
}