package com.example.reservationservice.controller;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import com.example.reservationservice.reservation.repository.QrTicketRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import com.example.reservationservice.reservation.scheduler.HoldExpirationScheduler;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private HoldExpirationScheduler holdExpirationScheduler;

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
    @DisplayName("앞선 HOLD가 만료되지 않은 채로는 대기열 1번이라도 결제로 정원을 초과할 수 없다")
    void queuedPaymentRejectedWhenSeatStillHeld() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        // 정원 채움 (HOLD, 아직 만료되지 않음 -> 실제 좌석은 비어있지 않음)
        mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 1)));

        // 대기열 1번
        MvcResult firstQueued = mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 1)))
                .andReturn();
        String firstReservationId = JsonPath.read(firstQueued.getResponse().getContentAsString(), "$.data.reservationId");

        // 대기열 순번(position==1)은 도달했지만, 실제 좌석은 아직 비어있지 않으므로 결제는 거부되어야 한다
        mockMvc.perform(post("/api/reservations/{id}/payment", firstReservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod": "CARD", "amount": 10000}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RESERVATION_SESSION_CAPACITY_EXCEEDED"));
    }

    @Test
    @DisplayName("HOLD가 만료되면 새 신청자가 아니라 대기열 1번이 좌석을 승계하고, 이어서 대기열 순번이 당겨진다")
    void queueFrontIsPromotedWhenHoldExpires_notNewApplicant() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        // 정원 채움 (HOLD)
        MvcResult firstHold = mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 1)))
                .andReturn();
        String firstReservationId = JsonPath.read(firstHold.getResponse().getContentAsString(), "$.data.reservationId");

        // 대기열 1번
        MvcResult secondQueued = mockMvc.perform(post("/api/reservations/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, UUID.randomUUID(), 1)))
                .andReturn();
        String secondReservationId = JsonPath.read(secondQueued.getResponse().getContentAsString(), "$.data.reservationId");

        // 대기열 2번
        MvcResult thirdQueued = mockMvc.perform(post("/api/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, UUID.randomUUID(), 1)))
                .andReturn();
        String thirdReservationId = JsonPath.read(thirdQueued.getResponse().getContentAsString(), "$.data.reservationId");

        // 1번 HOLD를 강제로 만료시키고 스케줄러 실행 (실제 10분 대기 대신 재현)
        Reservation firstReservation = reservationRepository.findById(UUID.fromString(firstReservationId)).orElseThrow();
        ReflectionTestUtils.setField(firstReservation, "expiresAt", LocalDateTime.now().minusMinutes(1));
        reservationRepository.saveAndFlush(firstReservation);

        holdExpirationScheduler.expireOverdueHolds();

        // 반납된 좌석은 새 신청자가 아니라 대기열 1번이 자동으로 승계해 HOLD가 되고, 대기열에서는 빠진다
        Reservation promoted = reservationRepository.findById(UUID.fromString(secondReservationId)).orElseThrow();
        assertThat(promoted.getStatus()).isEqualTo(ReservationStatus.HOLD);

        mockMvc.perform(get("/api/reservations/{id}/queue-position", secondReservationId))
                .andExpect(status().isNotFound());

        // 대기열 2번이었던 사람은 1번으로 당겨진다
        mockMvc.perform(get("/api/reservations/{id}/queue-position", thirdReservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        // 승계받은 사람은 정상적으로(HOLD 결제 경로로) 결제 가능하다
        mockMvc.perform(post("/api/reservations/{id}/payment", secondReservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod": "CARD", "amount": 10000}
                        """))
                .andExpect(status().isOk());

        // 좌석은 이미 승계자가 가져갔으므로, 순번이 1번으로 당겨졌다고 해도 대기열 3번이었던 사람은 결제할 수 없다
        mockMvc.perform(post("/api/reservations/{id}/payment", thirdReservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod": "CARD", "amount": 10000}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RESERVATION_SESSION_CAPACITY_EXCEEDED"));
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
