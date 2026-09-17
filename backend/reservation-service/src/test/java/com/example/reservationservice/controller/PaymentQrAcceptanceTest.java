package com.example.reservationservice.controller;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
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

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

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

    private RequestPostProcessor asUser(UUID memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, MemberRole.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

    @Test
    @DisplayName("정원 내 신청 건은 결제 완료 시 좌석이 확정되고 headcount만큼 QR이 발급된다.")
    void paymentConfirnsSeatAndIssuesQr() throws Exception {
        UUID sessionId = randomUUID();
        UUID memberId = randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        MvcResult holdResult = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, 2)))
                .andExpect(status().isCreated())
                .andReturn();


        String reservationId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(post("/api/reservations/{id}/payment", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(asUser(memberId))
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
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();
        UUID member3 = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        mockMvc.perform(post("/api/reservations/hold")
                .with(asUser(member1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, 1)));

        mockMvc.perform(post("/api/reservations/hold")
                .with(asUser(member2))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, 1)));

        MvcResult secondQueued = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(member3))
                        .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, 1)))
                .andReturn();

        String reservationId = JsonPath.read(secondQueued.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(post("/api/reservations/{id}/payment", reservationId)
                        .with(asUser(member3))
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
        UUID memberId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        MvcResult holdResult = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, 3)))
                .andReturn();

        String reservationId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(post("/api/reservations/{id}/payment", reservationId)
                .with(asUser(memberId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"paymentMethod": "CARD", "amount": 30000}
                    """));

        mockMvc.perform(get("/api/qr-tickets/{id}", reservationId)
                .with(asUser(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("결제 완료 전에는 좌석이 확정되지 않으며, QR 조회 시 404로 거부된다")
    void seatNotConfirmedAndQrTicketsNotFoundBeforePayment() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        MvcResult holdResult = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, 1)))
                .andReturn();

        String reservationId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(get("/api/qr-tickets/{id}", reservationId)
                        .with(asUser(memberId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("앞선 HOLD가 만료되지 않은 채로는 대기열 1번이라도 결제로 정원을 초과할 수 없다")
    void queuedPaymentRejectedWhenSeatStillHeld() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        mockMvc.perform(post("/api/reservations/hold")
                .with(asUser(member1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, 1)));

        MvcResult firstQueued = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(member2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, 1)))
                .andReturn();
        String firstReservationId = JsonPath.read(firstQueued.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(post("/api/reservations/{id}/payment", firstReservationId)
                        .with(asUser(member2))
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
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();
        UUID member3 = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        MvcResult firstHold = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(member1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, 1)))
                .andReturn();
        String firstReservationId = JsonPath.read(firstHold.getResponse().getContentAsString(), "$.data.reservationId");

        MvcResult secondQueued = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(member2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, 1)))
                .andReturn();
        String secondReservationId = JsonPath.read(secondQueued.getResponse().getContentAsString(), "$.data.reservationId");

        MvcResult thirdQueued = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(member3))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHoldJson(sessionId, 1)))
                .andReturn();
        String thirdReservationId = JsonPath.read(thirdQueued.getResponse().getContentAsString(), "$.data.reservationId");

        Reservation firstReservation = reservationRepository.findById(UUID.fromString(firstReservationId)).orElseThrow();
        ReflectionTestUtils.setField(firstReservation, "expiresAt", LocalDateTime.now().minusMinutes(1));
        reservationRepository.saveAndFlush(firstReservation);

        holdExpirationScheduler.expireOverdueHolds();

        Reservation promoted = reservationRepository.findById(UUID.fromString(secondReservationId)).orElseThrow();
        assertThat(promoted.getStatus()).isEqualTo(ReservationStatus.HOLD);

        mockMvc.perform(get("/api/reservations/{id}/queue-position", secondReservationId)
                        .with(asUser(member2)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/reservations/{id}/queue-position", thirdReservationId)
                        .with(asUser(member3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(post("/api/reservations/{id}/payment", secondReservationId)
                        .with(asUser(member2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"paymentMethod": "CARD", "amount": 10000}
                    """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reservations/{id}/payment", thirdReservationId)
                        .with(asUser(member3))
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
                .with(asUser(memberId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, 1)));

        UUID anotherSessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(anotherSessionId)).willReturn(10);

        mockMvc.perform(post("/api/reservations/hold")
                .with(asUser(memberId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(anotherSessionId, 2)));

        mockMvc.perform(get("/api/reservations/my")
                        .with(asUser(memberId)))
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
                .with(asUser(myMemberId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, 1)));

        mockMvc.perform(post("/api/reservations/hold")
                .with(asUser(otherMemberId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, 1)));

        mockMvc.perform(get("/api/reservations/my")
                        .with(asUser(myMemberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }


    @Test
    @DisplayName("세션 정원, 확정인원, 잔여좌석을 조회할 수 있다")
    void getCapacityStatus() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        mockMvc.perform(post("/api/reservations/hold")
                .with(asUser(member1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, 3)));

        mockMvc.perform(post("/api/reservations/hold")
                .with(asUser(member2))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createHoldJson(sessionId, 2)));

        mockMvc.perform(get("/api/reservations/sessions/{sessionId}/capacity-status", sessionId)
                .with(asUser(member1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(10))
                .andExpect(jsonPath("$.data.confirmedCount").value(5))
                .andExpect(jsonPath("$.data.remaining").value(5));
    }

    private String createHoldJson(UUID sessionId, int headCount) {
        StringBuilder attendees = new StringBuilder();
        for (int i = 0; i < headCount; i++) {
            if (i > 0) attendees.append(",");
            attendees.append("""
            {"ageGroup": "TWENTIES", "job": "DEVELOPER"}""");
        }
        return """
        {"sessionId": "%s", "headcount": %d, "attendees": [%s]}
        """.formatted(sessionId, headCount, attendees);
    }
}
