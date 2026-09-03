package com.example.reservationservice.controller;

import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
// Client 패키지 경로는 실제 프로젝트에 맞게 수정해주세요
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any; // 추가됨
import static org.mockito.BDDMockito.given; // 추가됨
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

    // 1. 외부 서비스 호출을 담당하는 Client를 Mocking 합니다.
    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @AfterEach
    void tearDown() {
        waitingQueueRepository.deleteAll();
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
    }

    @Test
    @DisplayName("정원 내 신청 시 홀드가 생성되고 201을 반환한다")
    void createHold_success() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        // 2. 가짜 응답 세팅: 해당 sessionId로 요청이 오면 정원 10명을 반환하도록 설정
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        String requestBody = """
                {
                    "sessionId": "%s",
                    "memberId": "%s",
                    "headcount": 1
                }
                """.formatted(sessionId, memberId);

        mockMvc.perform(post("/api/reservations/hold")
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

        // 2. 가짜 응답 세팅: 이 테스트에서도 정원을 10명으로 고정
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        for (int i = 0; i < 10; i++) {
            String body = """
                    {"sessionId": "%s", "memberId": "%s", "headcount": 1}
                    """.formatted(sessionId, UUID.randomUUID());
            mockMvc.perform(post("/api/reservations/hold")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
        }

        String overflowBody = """
                {"sessionId": "%s", "memberId": "%s", "headcount": 1}
                """.formatted(sessionId, UUID.randomUUID());

        mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overflowBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.queuePosition").value(1));
    }
}