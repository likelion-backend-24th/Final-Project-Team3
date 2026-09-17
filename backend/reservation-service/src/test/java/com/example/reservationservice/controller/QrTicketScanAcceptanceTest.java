package com.example.reservationservice.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.repository.ReservationRepository;
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
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class QrTicketScanAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QrTicketRepository qrTicketRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @AfterEach
    void tearDown() {
        qrTicketRepository.deleteAll();
        reservationRepository.deleteAll();
    }

    private RequestPostProcessor asOrganizer() {
        CustomUserDetails userDetails = new CustomUserDetails(UUID.randomUUID(), MemberRole.ORGANIZER);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

    private Reservation createReservation(UUID sessionId) {
        Reservation reservation = Reservation.builder()
                .sessionId(sessionId)
                .memberId(UUID.randomUUID())
                .headcount(1)
                .build();
        return reservationRepository.save(reservation);
    }

    @Test
    @DisplayName("유효한 QR 티켓은 세션 시작 이후 정상적으로 스캔되어 입장 처리된다")
    void 정상_스캔() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Reservation reservation = createReservation(sessionId);
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().minusHours(1));

        QrTicket ticket = QrTicket.builder()
                .reservationId(reservation.getId())
                .code("VALID-CODE-1")
                .build();
        qrTicketRepository.save(ticket);

        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "VALID-CODE-1")
                        .with(asOrganizer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.used").value(true));
    }

    @Test
    @DisplayName("세션 시작 전에는 스캔이 거부된다")
    void 세션_시작전_스캔_거부() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Reservation reservation = createReservation(sessionId);
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().plusHours(1));

        QrTicket ticket = QrTicket.builder()
                .reservationId(reservation.getId())
                .code("FUTURE-CODE")
                .build();
        qrTicketRepository.save(ticket);

        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "FUTURE-CODE")
                        .with(asOrganizer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("QR_TICKET_SESSION_NOT_STARTED"));
    }

    @Test
    @DisplayName("존재하지 않는 코드는 404로 거부된다")
    void 존재하지않는_코드_404() throws Exception {
        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "NOT-EXIST")
                        .with(asOrganizer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("이미 사용된 코드는 409로 거부된다")
    void 이미사용된_코드_409() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Reservation reservation = createReservation(sessionId);
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().minusHours(1));

        QrTicket ticket = QrTicket.builder()
                .reservationId(reservation.getId())
                .code("USED-CODE")
                .build();
        ticket.scan();
        qrTicketRepository.save(ticket);

        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "USED-CODE")
                        .with(asOrganizer()))
                .andExpect(status().isConflict());
    }
}