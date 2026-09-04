package com.example.reservationservice.controller;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
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

    @Test
    @DisplayName("정원 초과 시 대기열에 등록 순서대로 순번이 부여된다")
    void queueRegistrationOrderTest() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(sessionId, UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("HOLD"));

        mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(sessionId, UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.queuePosition").value(1));

        mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(sessionId, UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.queuePosition").value(2));
    }

    @Test
    @DisplayName("대기열 1번째는 순번이 도달했다고 판단한다")
    void queuePositionReachedTest() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        // 정원 채움
        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(sessionId, UUID.randomUUID())));

        // 대기열 1번째 등록
        MvcResult queuedResult = mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(sessionId, UUID.randomUUID())))
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

        // 정원 채움
        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(sessionId, UUID.randomUUID())));

        // 대기열 1번째 등록
        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(sessionId, UUID.randomUUID())));

        // 대기열 2번째 등록 (테스트 대상)
        MvcResult secondQueuedResult = mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(sessionId, UUID.randomUUID())))
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

        String requestJson = createRequestJson(sessionId, memberId);

        // 첫 번째 신청 -> 성공
        mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // 같은 사람이 같은 세션에 다시 신청 -> 거부
        mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RESERVATION_DUPLICATE"));
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