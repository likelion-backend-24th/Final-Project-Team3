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

    @Test
    @DisplayName("결제 완료 후 QR 티켓 목록을 조회할 수 있다")
    void getQrTicketsAfterPayment() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        MvcResult holdResult = mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, UUID.randomUUID(), 3)))
                .andReturn();

        String reservationId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(post("/api/reservations/{id}/payment", reservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"paymentMethod": "CARD", "amount": 30000}
                    """));

        mockMvc.perform(get("/api/reservations/{id}/qr-tickets", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("결제 완료 전에는 좌석이 확정되지 않으며, QR 조회 시 404로 거부된다")
    void seatNotConfirmedAndQrTicketsNotFoundBeforePayment() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        MvcResult holdResult = mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, UUID.randomUUID(), 1)))
                .andReturn();

        String reservationId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(get("/api/reservations/{id}/qr-tickets", reservationId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("대기열 1번이 결제 완료하면, 2번의 순번이 1번으로 당겨져 결제가 가능해진다")
    void queuePositionAdvancesAfterPayment() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        // 정원 채움 (HOLD)
        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 1)));

        // 대기열 1번
        MvcResult firstQueued = mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 1)))
                .andReturn();
        String firstReservationId = JsonPath.read(firstQueued.getResponse().getContentAsString(), "$.data.reservationId");

        // 대기열 2번
        MvcResult secondQueued = mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, UUID.randomUUID(), 1)))
                .andReturn();
        String secondReservationId = JsonPath.read(secondQueued.getResponse().getContentAsString(), "$.data.reservationId");

        // 2번 순서는 미도달 -> 결제 거부
        mockMvc.perform(post("/api/reservations/{id}/payment", secondReservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod": "CARD", "amount": 10000}
                        """))
                .andExpect(status().isForbidden());

        // 1번이 결제 완료 (대기열에서 빠짐)
        mockMvc.perform(post("/api/reservations/{id}/payment", firstReservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod": "CARD", "amount" : 10000}
                        """))
                .andExpect(status().isOk());

        // 2번 조회 시 1번으로 당겨져 있어야 함
        mockMvc.perform(get("/api/reservations/{id}/queue-position", secondReservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        // 2번이었던 사람도  결제 가능해야 함
        mockMvc.perform(post("/api/reservations/{id}/payment", secondReservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod": "CARD", "amount": 10000}
                        """))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("회원은 자신의 예약목록을 최신순을 조회 할 수 있다.")
    void getMyReservations() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sessionId": "%s", "memberId": "%s", "headcount": 1}
                        """.formatted(sessionId, memberId)));

        UUID anotherSessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(anotherSessionId)).willReturn(10);

        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sessionId": "%s", "memberId": "%s", "headcount": 2}
                        """.formatted(anotherSessionId, memberId)));
        mockMvc.perform(get("/api/reservations/my")
                .param("memberId", memberId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("다른 회원의 예약은 조회 결과에 포함되지 않는다")
    void getMyReservationExcludesOthers() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID myMemberId = UUID.randomUUID();
        UUID otherMemberId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sessionId": "%s", "memberId": "%s", "headcount": 1}
                        """.formatted(sessionId, myMemberId)));

        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sessionId": "%s", "memberId": "%s", "headcount": 1}
                    """.formatted(sessionId, otherMemberId)));

        mockMvc.perform(get("/api/reservations/my")
                .param("memberId", myMemberId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("세션 정원, 확정인원, 잔여좌석을 조회할 수 있다")
    void getCapacityStatus() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 3)));

        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 2)));

        mockMvc.perform(get("/api/reservations/sessions/{sessionId}/capacity-status", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(10))
                .andExpect(jsonPath("$.data.confirmedCount").value(5))
                .andExpect(jsonPath("$.data.remaining").value(5));
    }

    private String createHoldJson(UUID sessionId, UUID memberId, int headCount) {
        return """
                {"sessionId": "%s", "memberId": "%s", "headcount": %d}
                """.formatted(sessionId, memberId, headCount);
    }
}
