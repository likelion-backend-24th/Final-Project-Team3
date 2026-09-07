package com.example.reservationservice.controller;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import com.example.reservationservice.reservation.repository.QrTicketRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import com.jayway.jsonpath.JsonPath;
import org.hibernate.query.sql.internal.ParameterRecognizerImpl;
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

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentQrAcceptanceTest {

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

    @Autowired
    private QrTicketRepository qrTicketRepository;

    @BeforeEach
    void setUp() {
        qrTicketRepository.deleteAll();
        waitingQueueRepository.deleteAll();
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
        ;
    }

    @Test
    @DisplayName("정원 내 신청 건은 결제 완료 시 좌석이 확정되고 headcount만큼 QR이 발급된다.")
    void paymentConfirnsSeatAndIssuesQr() throws Exception {
        UUID sessionId = randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        MvcResult holdResult = mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, UUID.randomUUID(), 2)))
                .andExpect(status().isCreated())
                .andReturn();


        String reservationId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(post("/api/reservations/{id}/payment", reservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod": "CARD", "amount": 20000}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.qrTicketCount").value(2));


    }

    @Test
    @DisplayName("대기열 순번 미도달 상태에서 결제 시 403을 반환한다")
    void paymentRejectedWhenQueuePositionNotReached() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 1)));

        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 1)));

        MvcResult secondQueued = mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 1)))
                .andReturn();

        String reservationId = JsonPath.read(secondQueued.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(post("/api/reservations/{id}/payment", reservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod": "CARD", "amount": 10000}
                        """))
                .andExpect(status().isForbidden());
    }

    private String createHoldJson(UUID sessionId, UUID memberId, int headCount) {
        return """
                {"sessionId": "%s", "memberId": "%s", "headcount": %d}
                """.formatted(sessionId, memberId, headCount);
    }
}
