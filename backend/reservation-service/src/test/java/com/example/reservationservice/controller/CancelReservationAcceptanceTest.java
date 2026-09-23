package com.example.reservationservice.controller;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.entity.SessionCapacityLock;
import com.example.reservationservice.reservation.entity.WaitingQueue;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.payment.repository.PaymentRepository;
import com.example.reservationservice.payment.service.PortOnePaymentVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @MockitoBean
    private PortOnePaymentVerifier portOnePaymentVerifier;

    private RequestPostProcessor asUser(UUID memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, MemberRole.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

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

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundRate").value(100))
                .andExpect(jsonPath("$.data.refundAmount").value(10000));

        verify(portOnePaymentVerifier).cancel(eq(reservation.getId().toString()), eq(10000), anyString());
    }

    @Test
    @DisplayName("세션 시작 3~6일 전 취소 시 50% 환불된다")
    void 세션시작_3에서6일전_50퍼센트_환불() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().plusDays(5));

        Reservation reservation = createConfirmedReservation(sessionId);
        createPayment(reservation.getId(), 10000);

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId())
                .with(asUser(reservation.getMemberId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundRate").value(50))
                .andExpect(jsonPath("$.data.refundAmount").value(5000));

        verify(portOnePaymentVerifier).cancel(eq(reservation.getId().toString()), eq(5000), anyString());
    }

    @Test
    @DisplayName("세션 시작 3일 미만 취소 시 환불되지 않는다")
    void 세션시작_3일미만_환불불가() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().plusDays(1));

        Reservation reservation = createConfirmedReservation(sessionId);
        createPayment(reservation.getId(), 10000);

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundRate").value(0))
                .andExpect(jsonPath("$.data.refundAmount").value(0));

        verify(portOnePaymentVerifier, never()).cancel(anyString(), any(), anyString());
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

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundRate").doesNotExist());
    }

    @Test
    @DisplayName("정원이 꽉 찬 세션에서 예약을 취소하면 대기열 1번이 즉시 HOLD로 승격된다")
    void 취소시_대기열_1번이_즉시_승격된다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().plusDays(10));
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(1);

        // 정원 1인 세션이 이미 1명으로 꽉 찬 상태를 만든다(current_active=1)
        SessionCapacityLock lock = new SessionCapacityLock(sessionId);
        lock.increase(1);
        sessionCapacityLockRepository.save(lock);

        Reservation confirmed = createConfirmedReservation(sessionId);
        createPayment(confirmed.getId(), 10000);

        Reservation queued = Reservation.builder()
                .sessionId(sessionId)
                .memberId(UUID.randomUUID())
                .headcount(1)
                .build();
        queued.markAsQueued();
        reservationRepository.save(queued);
        waitingQueueRepository.save(WaitingQueue.builder()
                .reservationId(queued.getId())
                .sessionId(sessionId)
                .memberId(queued.getMemberId())
                .position(1)
                .build());

        mockMvc.perform(post("/api/reservations/{id}/cancel", confirmed.getId())
                        .with(asUser(confirmed.getMemberId())))
                .andExpect(status().isOk());

        Reservation promoted = reservationRepository.findById(queued.getId()).orElseThrow();
        assertThat(promoted.getStatus()).isEqualTo(ReservationStatus.HOLD);
        assertThat(waitingQueueRepository.findByReservationId(queued.getId())).isEmpty();
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

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId())
                        .with(asUser(reservation.getMemberId())))
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