package com.example.reservationservice.controller;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.entity.WaitingQueue;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.payment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CancelReservationAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        waitingQueueRepository.deleteAll();
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
    }

    @Test
    @DisplayName("세션 시작 7일 이상 전 취소 시 100% 환불된다")
    void 세션시작_7일이상_100퍼센트_환불() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().plusDays(10));

        Reservation reservation = createConfirmedReservation(sessionId);
        createPayment(reservation.getId(), 10000);

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundRate").value(100))
                .andExpect(jsonPath("$.data.refundAmount").value(10000));
    }

    @Test
    @DisplayName("세션 시작 3~6일 전 취소 시 50% 환불된다")
    void 세션시작_3에서6일전_50퍼센트_환불() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().plusDays(5));

        Reservation reservation = createConfirmedReservation(sessionId);
        createPayment(reservation.getId(), 10000);

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundRate").value(50))
                .andExpect(jsonPath("$.data.refundAmount").value(5000));
    }

    @Test
    @DisplayName("세션 시작 3일 미만 취소 시 환불되지 않는다")
    void 세션시작_3일미만_환불불가() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().plusDays(1));

        Reservation reservation = createConfirmedReservation(sessionId);
        createPayment(reservation.getId(), 10000);

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundRate").value(0))
                .andExpect(jsonPath("$.data.refundAmount").value(0));
    }

    @Test
    @DisplayName("HOLD 상태는 환불 없이 즉시 취소된다")
    void HOLD상태_즉시취소() throws Exception {
        UUID sessionId = UUID.randomUUID();

        Reservation reservation = Reservation.builder()
                .sessionId(sessionId)
                .memberId(UUID.randomUUID())
                .headcount(1)
                .build();
        reservationRepository.save(reservation);

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundRate").doesNotExist());
    }

    @Test
    @DisplayName("이미 취소된 예약을 재요청하면 409로 거부된다")
    void 이미취소된_예약_재요청시_409() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
                .sessionId(sessionId)
                .memberId(UUID.randomUUID())
                .headcount(1)
                .build();
        reservationRepository.save(reservation);
        reservation.markAsCancelled();
        reservationRepository.saveAndFlush(reservation);

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                .andExpect(status().isConflict());
    }

    private Reservation createConfirmedReservation(UUID sessionId) {
        Reservation reservation = Reservation.builder()
                .sessionId(sessionId)
                .memberId(UUID.randomUUID())
                .headcount(1)
                .build();
        reservationRepository.save(reservation);
        ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
        reservationRepository.saveAndFlush(reservation);
        return reservation;
    }

    private void createPayment(UUID reservationId, int amount) {
        Payment payment = Payment.builder()
                .reservationId(reservationId)
                .amount(amount)
                .paymentMethod("CARD")
                .build();
        paymentRepository.save(payment);
    }
}