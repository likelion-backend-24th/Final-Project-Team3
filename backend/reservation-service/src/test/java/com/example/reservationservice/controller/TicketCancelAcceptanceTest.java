package com.example.reservationservice.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.payment.repository.PaymentRepository;
import com.example.reservationservice.payment.service.PortOnePaymentVerifier;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.entity.SessionCapacityLock;
import com.example.reservationservice.reservation.entity.WaitingQueue;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Task 12-5: 한 예약에 여러 명이 묶여 있을 때 QR 티켓 1장(=1명) 단위로 취소·부분 환불한다.
@SpringBootTest
@AutoConfigureMockMvc
class TicketCancelAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private QrTicketRepository qrTicketRepository;

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @MockitoBean
    private PortOnePaymentVerifier portOnePaymentVerifier;

    @AfterEach
    void tearDown() {
        qrTicketRepository.deleteAll();
        paymentRepository.deleteAll();
        waitingQueueRepository.deleteAll();
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
    }

    @Test
    @DisplayName("3명 예약에서 1명만 취소하면 그 1명분만 환불되고 나머지는 그대로 남는다")
    void 부분_환불_100퍼센트() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId)).willReturn(LocalDateTime.now().plusDays(10));
        given(conferenceServiceClient.getSessionPrice(sessionId)).willReturn(5000);

        Reservation reservation = createConfirmedReservation(sessionId, 3);
        createPayment(reservation.getId(), 15000);
        List<QrTicket> tickets = createTickets(reservation.getId(), 3, 0);

        mockMvc.perform(post("/api/reservations/{rid}/tickets/{tid}/cancel", reservation.getId(), tickets.get(0).getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundRate").value(100))
                .andExpect(jsonPath("$.data.refundAmount").value(5000))
                .andExpect(jsonPath("$.data.remainingHeadcount").value(2));

        verify(portOnePaymentVerifier).cancel(eq(reservation.getId().toString()), eq(5000), anyString());
        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getHeadcount()).isEqualTo(2);
        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(qrTicketRepository.findByReservationId(reservation.getId())).hasSize(2);
    }

    @Test
    @DisplayName("세션 시작 3~6일 전이면 50% 환불된다")
    void 부분_환불_50퍼센트() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId)).willReturn(LocalDateTime.now().plusDays(5));
        given(conferenceServiceClient.getSessionPrice(sessionId)).willReturn(5000);

        Reservation reservation = createConfirmedReservation(sessionId, 2);
        createPayment(reservation.getId(), 10000);
        List<QrTicket> tickets = createTickets(reservation.getId(), 2, 0);

        mockMvc.perform(post("/api/reservations/{rid}/tickets/{tid}/cancel", reservation.getId(), tickets.get(0).getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundRate").value(50))
                .andExpect(jsonPath("$.data.refundAmount").value(2500));
    }

    @Test
    @DisplayName("세션 시작 3일 미만이면 환불 없이 취소만 된다")
    void 부분_환불_0퍼센트() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId)).willReturn(LocalDateTime.now().plusDays(1));
        given(conferenceServiceClient.getSessionPrice(sessionId)).willReturn(5000);

        Reservation reservation = createConfirmedReservation(sessionId, 2);
        createPayment(reservation.getId(), 10000);
        List<QrTicket> tickets = createTickets(reservation.getId(), 2, 0);

        mockMvc.perform(post("/api/reservations/{rid}/tickets/{tid}/cancel", reservation.getId(), tickets.get(0).getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundRate").value(0))
                .andExpect(jsonPath("$.data.refundAmount").value(0));

        verify(portOnePaymentVerifier, never()).cancel(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("체크인된(used=true) 티켓은 취소할 수 없다")
    void 체크인된_티켓은_거부된다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId)).willReturn(LocalDateTime.now().plusDays(10));

        Reservation reservation = createConfirmedReservation(sessionId, 2);
        createPayment(reservation.getId(), 10000);
        List<QrTicket> tickets = createTickets(reservation.getId(), 2, 1);

        mockMvc.perform(post("/api/reservations/{rid}/tickets/{tid}/cancel", reservation.getId(), tickets.get(0).getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("남은 유효 티켓이 1장뿐이면 개별 취소가 거부된다")
    void 마지막_한장은_거부된다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId)).willReturn(LocalDateTime.now().plusDays(10));

        Reservation reservation = createConfirmedReservation(sessionId, 1);
        createPayment(reservation.getId(), 5000);
        List<QrTicket> tickets = createTickets(reservation.getId(), 1, 0);

        mockMvc.perform(post("/api/reservations/{rid}/tickets/{tid}/cancel", reservation.getId(), tickets.get(0).getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("본인 소유 예약이 아니면 403이다")
    void 본인아니면_403() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId)).willReturn(LocalDateTime.now().plusDays(10));

        Reservation reservation = createConfirmedReservation(sessionId, 2);
        createPayment(reservation.getId(), 10000);
        List<QrTicket> tickets = createTickets(reservation.getId(), 2, 0);

        mockMvc.perform(post("/api/reservations/{rid}/tickets/{tid}/cancel", reservation.getId(), tickets.get(0).getId())
                        .with(asUser(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("한 예약에서 두 명을 순차로 개별 취소하면 환불액이 누적된다")
    void 환불액이_누적된다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId)).willReturn(LocalDateTime.now().plusDays(10));
        given(conferenceServiceClient.getSessionPrice(sessionId)).willReturn(5000);

        Reservation reservation = createConfirmedReservation(sessionId, 3);
        createPayment(reservation.getId(), 15000);
        List<QrTicket> tickets = createTickets(reservation.getId(), 3, 0);

        mockMvc.perform(post("/api/reservations/{rid}/tickets/{tid}/cancel", reservation.getId(), tickets.get(0).getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/reservations/{rid}/tickets/{tid}/cancel", reservation.getId(), tickets.get(1).getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isOk());

        Payment payment = paymentRepository.findByReservationId(reservation.getId()).orElseThrow();
        assertThat(payment.getRefundedAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("정원이 꽉 찬 세션에서 개별 취소하면 대기열 1번이 즉시 승격된다")
    void 개별취소시_대기열이_승격된다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionStartAt(sessionId)).willReturn(LocalDateTime.now().plusDays(10));
        given(conferenceServiceClient.getSessionPrice(sessionId)).willReturn(5000);
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(2);

        SessionCapacityLock lock = new SessionCapacityLock(sessionId);
        lock.increase(2);
        sessionCapacityLockRepository.save(lock);

        Reservation reservation = createConfirmedReservation(sessionId, 2);
        createPayment(reservation.getId(), 10000);
        List<QrTicket> tickets = createTickets(reservation.getId(), 2, 0);

        Reservation queued = Reservation.builder().sessionId(sessionId).memberId(UUID.randomUUID()).headcount(1).build();
        queued.markAsQueued();
        reservationRepository.save(queued);
        waitingQueueRepository.save(WaitingQueue.builder()
                .reservationId(queued.getId()).sessionId(sessionId).memberId(queued.getMemberId()).position(1).build());

        mockMvc.perform(post("/api/reservations/{rid}/tickets/{tid}/cancel", reservation.getId(), tickets.get(0).getId())
                        .with(asUser(reservation.getMemberId())))
                .andExpect(status().isOk());

        assertThat(reservationRepository.findById(queued.getId()).orElseThrow().getStatus()).isEqualTo(ReservationStatus.HOLD);
    }

    private RequestPostProcessor asUser(UUID memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, MemberRole.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

    private Reservation createConfirmedReservation(UUID sessionId, int headcount) {
        Reservation reservation = Reservation.builder()
                .sessionId(sessionId)
                .memberId(UUID.randomUUID())
                .headcount(headcount)
                .build();
        reservation.markAsConfirmed();
        reservationRepository.save(reservation);
        return reservation;
    }

    private void createPayment(UUID reservationId, int amount) {
        paymentRepository.save(Payment.builder()
                .reservationId(reservationId)
                .amount(amount)
                .paymentMethod("CARD")
                .build());
    }

    private List<QrTicket> createTickets(UUID reservationId, int count, int usedCount) {
        List<QrTicket> saved = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            QrTicket ticket = QrTicket.builder()
                    .reservationId(reservationId)
                    .code(UUID.randomUUID().toString().replace("-", ""))
                    .ageGroup(AgeGroup.TWENTIES)
                    .job(Job.DEVELOPER)
                    .build();
            if (i < usedCount) {
                ticket.markAsUsed();
            }
            saved.add(qrTicketRepository.save(ticket));
        }
        return saved;
    }
}
