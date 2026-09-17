package com.example.reservationservice.settlement.controller;

import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.payment.repository.PaymentRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
class SettlementAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
    }

    @Test
    void getSettlementDashboard_asAdmin_sumsConfirmedAndExcludesCancelled() throws Exception {
        confirmedReservationWithPayment(10000);
        confirmedReservationWithPayment(20000);
        cancelledReservationWithPayment(99999);

        mockMvc.perform(get("/api/admin/settlements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(30000));
    }

    @Test
    void getSettlementDashboard_withoutAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/settlements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken()))
                .andExpect(status().isForbidden());
    }

    private void confirmedReservationWithPayment(int amount) {
        Reservation reservation = reservationRepository.save(hold());
        reservation.markAsConfirmed();
        reservationRepository.save(reservation);
        paymentRepository.save(Payment.builder()
                .reservationId(reservation.getId())
                .amount(amount)
                .paymentMethod("CARD")
                .build());
    }

    private void cancelledReservationWithPayment(int amount) {
        Reservation reservation = reservationRepository.save(hold());
        reservation.markAsConfirmed();
        reservation.markAsCancelled();
        reservationRepository.save(reservation);
        paymentRepository.save(Payment.builder()
                .reservationId(reservation.getId())
                .amount(amount)
                .paymentMethod("CARD")
                .build());
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
