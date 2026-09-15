package com.example.reservationservice.controller;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
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
public class ReservationStatusSummaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private QrTicketRepository qrTicketRepository;

    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @AfterEach
    void tearDown() {
        qrTicketRepository.deleteAll();
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
    }

    @Test
    @DisplayName("상태별 건수 합이 전체 신청 건수와 일치한다")
    void 상태별_건수_합이_전체와_일치한다() throws Exception {
        UUID sessionId = UUID.randomUUID();

        createReservation(sessionId, ReservationStatus.HOLD);
        createReservation(sessionId, ReservationStatus.QUEUED);
        createReservation(sessionId, ReservationStatus.CONFIRMED);
        createReservation(sessionId, ReservationStatus.CANCELLED);

        mockMvc.perform(get("/api/reservations/sessions/{sessionId}/status-summary", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.holdCount").value(1))
                .andExpect(jsonPath("$.data.queuedCount").value(1))
                .andExpect(jsonPath("$.data.confirmedCount").value(1))
                .andExpect(jsonPath("$.data.cancelledCount").value(1));
    }

    @Test
    @DisplayName("체크인 완료된 QR 티켓만 집계에 포함된다")
    void 체크인_완료된_티켓만_집계된다() throws Exception {
        UUID sessionId = UUID.randomUUID();

        Reservation reservation = createReservation(sessionId, ReservationStatus.CONFIRMED);

        QrTicket usedTicket = QrTicket.builder()
                .reservationId(reservation.getId())
                .code(UUID.randomUUID().toString())
                .build();
        usedTicket.markAsUsed();
        qrTicketRepository.save(usedTicket);

        QrTicket unusedTicket = QrTicket.builder()
                .reservationId(reservation.getId())
                .code(UUID.randomUUID().toString())
                .build();
        qrTicketRepository.save(unusedTicket);

        mockMvc.perform(get("/api/reservations/sessions/{sessionId}/status-summary", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedInCount").value(1));
    }

    private Reservation createReservation(UUID sessionId, ReservationStatus status) {
        Reservation reservation = Reservation.builder()
                .sessionId(sessionId)
                .memberId(UUID.randomUUID())
                .headcount(1)
                .build();
        reservationRepository.save(reservation);
        ReflectionTestUtils.setField(reservation, "status", status);
        reservationRepository.saveAndFlush(reservation);
        return reservation;
    }
}