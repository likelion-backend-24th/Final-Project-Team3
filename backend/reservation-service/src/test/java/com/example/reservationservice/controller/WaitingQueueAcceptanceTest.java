package com.example.reservationservice.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import com.example.reservationservice.reservation.service.ReservationService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WaitingQueueAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

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
    @DisplayName("정원 초과 시 대기열에 등록 순서대로 순번이 부여된다")
    void queueRegistrationOrderTest() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(sessionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("HOLD"));

        mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(sessionId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.queuePosition").value(1));

        mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(sessionId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.queuePosition").value(2));
    }

    @Test
    @DisplayName("대기열 1번째는 순번이 도달했다고 판단한다")
    void queuePositionReachedTest() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        mockMvc.perform(post("/api/reservations/hold")
                .with(asUser(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(sessionId)));

        MvcResult queuedResult = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(sessionId)))
                .andReturn();

        String responseBody = queuedResult.getResponse().getContentAsString();
        String reservationId = JsonPath.read(responseBody, "$.data.reservationId");

        boolean reached = reservationService.isQueuePositionReached(UUID.fromString(reservationId));
        assertThat(reached).isTrue();
    }

    @Test
    @DisplayName("대기열 2번째 이후는 순번이 도달하지 않았다고 판단한다")
    void queuePositionNotReachedTest() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        mockMvc.perform(post("/api/reservations/hold")
                .with(asUser(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(sessionId)));

        mockMvc.perform(post("/api/reservations/hold")
                .with(asUser(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(sessionId)));

        MvcResult secondQueuedResult = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(sessionId)))
                .andReturn();

        String responseBody = secondQueuedResult.getResponse().getContentAsString();
        String reservationId = JsonPath.read(responseBody, "$.data.reservationId");

        boolean reached = reservationService.isQueuePositionReached(UUID.fromString(reservationId));
        assertThat(reached).isFalse();
    }

    @Test
    @DisplayName("같은 회원이 같은 세션에 중복 신청하면 거부된다")
    void duplicateReservationRejectedTest() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        String requestJson = createRequestJson(sessionId);

        mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RESERVATION_DUPLICATE"));
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