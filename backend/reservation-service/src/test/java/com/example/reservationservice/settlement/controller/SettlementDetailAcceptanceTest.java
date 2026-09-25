package com.example.reservationservice.settlement.controller;

import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.payment.repository.PaymentRepository;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Task 18-3: 정산 내역 상세 조회. 멘토링 피드백("어느 것을 결제했고, 취소환불내역, 입장했는지")을
// 그대로 검증한다 — 취소건 포함 여부, 환불 정보, 체크인 집계, 페이지네이션.
@SpringBootTest
@AutoConfigureMockMvc
class SettlementDetailAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private QrTicketRepository qrTicketRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        qrTicketRepository.deleteAll();
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
    }

    @Test
    void 확정건과_취소건이_모두_상태와_환불정보와_함께_조회된다() throws Exception {
        confirmedWithCheckedIn(20000, 2, 1);
        cancelledWithRefund(10000, 5000);

        mockMvc.perform(get("/api/admin/settlements/details")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.meta.pagination.totalItems").value(2))
                .andExpect(jsonPath("$.data[0].reservationStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data[0].refundedAmount").value(5000))
                .andExpect(jsonPath("$.data[1].reservationStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data[1].refundedAmount").doesNotExist())
                .andExpect(jsonPath("$.data[1].ticketCount").value(2))
                .andExpect(jsonPath("$.data[1].checkedInCount").value(1));
    }

    @Test
    void 페이지_크기만큼만_반환된다() throws Exception {
        confirmedWithCheckedIn(1000, 1, 0);
        confirmedWithCheckedIn(2000, 1, 0);
        confirmedWithCheckedIn(3000, 1, 0);

        mockMvc.perform(get("/api/admin/settlements/details")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.meta.pagination.totalItems").value(3))
                .andExpect(jsonPath("$.meta.pagination.totalPages").value(2));
    }

    @Test
    void 관리자가_아니면_403이다() throws Exception {
        mockMvc.perform(get("/api/admin/settlements/details")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isForbidden());
    }

    private void confirmedWithCheckedIn(int amount, int ticketCount, int checkedInCount) {
        Reservation reservation = reservationRepository.save(hold());
        reservation.markAsConfirmed();
        reservationRepository.save(reservation);
        paymentRepository.save(Payment.builder()
                .reservationId(reservation.getId())
                .amount(amount)
                .paymentMethod("CARD")
                .build());
        for (int i = 0; i < ticketCount; i++) {
            QrTicket ticket = QrTicket.builder()
                    .reservationId(reservation.getId())
                    .code(UUID.randomUUID().toString().replace("-", ""))
                    .ageGroup(AgeGroup.TWENTIES)
                    .job(Job.DEVELOPER)
                    .build();
            if (i < checkedInCount) {
                ticket.scan();
            }
            qrTicketRepository.save(ticket);
        }
    }

    private void cancelledWithRefund(int amount, int refundedAmount) {
        Reservation reservation = reservationRepository.save(hold());
        reservation.markAsConfirmed();
        reservation.markAsCancelled();
        reservationRepository.save(reservation);
        Payment payment = Payment.builder()
                .reservationId(reservation.getId())
                .amount(amount)
                .paymentMethod("CARD")
                .build();
        payment.recordRefund(refundedAmount);
        paymentRepository.save(payment);
    }

    private Reservation hold() {
        return Reservation.builder()
                .sessionId(UUID.randomUUID())
                .memberId(UUID.randomUUID())
                .headcount(1)
                .build();
    }

    private String adminToken() {
        return token(MemberRole.ADMIN);
    }

    private String organizerToken() {
        return token(MemberRole.ORGANIZER);
    }

    private String token(MemberRole role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }
}
