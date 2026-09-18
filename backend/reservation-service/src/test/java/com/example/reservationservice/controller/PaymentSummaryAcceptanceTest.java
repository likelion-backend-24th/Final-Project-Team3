package com.example.reservationservice.controller;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.payment.repository.PaymentRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentSummaryAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
    }

    @Test
    @DisplayName("여러 세션의 결제완료 금액을 정확히 집계한다")
    void 결제완료_금액_정확히_집계된다() throws Exception {
        UUID sessionId1 = UUID.randomUUID();
        UUID sessionId2 = UUID.randomUUID();

        Reservation r1 = createConfirmedReservation(sessionId1);
        savePayment(r1.getId(), 10000);

        Reservation r2 = createConfirmedReservation(sessionId2);
        savePayment(r2.getId(), 20000);

        mockMvc.perform(get("/internal/sessions/payment-summary")
                        .param("sessionIds", sessionId1.toString(), sessionId2.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(30000))
                .andExpect(jsonPath("$.data.confirmedCount").value(2))
                .andExpect(jsonPath("$.data.netRevenue").value(30000))
                .andExpect(jsonPath("$.data.cancelledCount").value(0));
    }

    @Test
    @DisplayName("취소된 건은 매출 집계에서 제외된다")
    void 취소건은_매출에서_제외된다() throws Exception {
        UUID sessionId = UUID.randomUUID();

        Reservation confirmed = createConfirmedReservation(sessionId);
        savePayment(confirmed.getId(), 10000);

        Reservation cancelled = createConfirmedReservation(sessionId);
        Payment cancelledPayment = savePayment(cancelled.getId(), 5000);
        cancelledPayment.recordRefund(5000);
        paymentRepository.saveAndFlush(cancelledPayment);
        ReflectionTestUtils.setField(cancelled, "status", ReservationStatus.CANCELLED);
        reservationRepository.saveAndFlush(cancelled);

        mockMvc.perform(get("/internal/sessions/payment-summary")
                        .param("sessionIds", sessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(10000))
                .andExpect(jsonPath("$.data.confirmedCount").value(1))
                .andExpect(jsonPath("$.data.refundedAmount").value(5000))
                .andExpect(jsonPath("$.data.cancelledCount").value(1))
                .andExpect(jsonPath("$.data.netRevenue").value(5000));
    }

    @Test
    @DisplayName("결제 건이 없는 세션은 0원으로 집계된다")
    void 결제없는_세션은_0원() throws Exception {
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(get("/internal/sessions/payment-summary")
                        .param("sessionIds", sessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(0))
                .andExpect(jsonPath("$.data.confirmedCount").value(0));
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

    private Payment savePayment(UUID reservationId, int amount) {
        Payment payment = Payment.builder()
                .reservationId(reservationId)
                .amount(amount)
                .paymentMethod("CARD")
                .build();
        return paymentRepository.save(payment);
    }
}