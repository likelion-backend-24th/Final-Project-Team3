package com.example.reservationservice.controller;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AttendeeCheckinStatsAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private QrTicketRepository qrTicketRepository;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @AfterEach
    void tearDown() {
        qrTicketRepository.deleteAll();
        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("체크인 완료된 좌석만 연령대·직무별로 집계된다")
    void 체크인_완료_좌석만_집계된다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Reservation reservation = createReservation(sessionId);

        saveCheckedInTicket(reservation.getId(), AgeGroup.TWENTIES, Job.DEVELOPER);
        saveCheckedInTicket(reservation.getId(), AgeGroup.THIRTIES, Job.DESIGNER);

        mockMvc.perform(get("/internal/sessions/attendee-checkin-stats")
                        .param("sessionIds", sessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedInCount").value(2))
                .andExpect(jsonPath("$.data.ageGroupDistribution.TWENTIES").value(1))
                .andExpect(jsonPath("$.data.ageGroupDistribution.THIRTIES").value(1))
                .andExpect(jsonPath("$.data.jobDistribution.DEVELOPER").value(1))
                .andExpect(jsonPath("$.data.jobDistribution.DESIGNER").value(1));
    }

    @Test
    @DisplayName("체크인(QR 사용) 안 된 좌석은 집계에서 제외된다")
    void 미체크인_좌석은_제외된다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Reservation reservation = createReservation(sessionId);

        saveCheckedInTicket(reservation.getId(), AgeGroup.TWENTIES, Job.DEVELOPER);
        saveUncheckedTicket(reservation.getId(), AgeGroup.FORTIES, Job.PLANNER_PM);

        mockMvc.perform(get("/internal/sessions/attendee-checkin-stats")
                        .param("sessionIds", sessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedInCount").value(1))
                .andExpect(jsonPath("$.data.ageGroupDistribution.TWENTIES").value(1))
                .andExpect(jsonPath("$.data.ageGroupDistribution.FORTIES").doesNotExist());
    }

    @Test
    @DisplayName("요청한 세션에 속하지 않는 예약의 체크인은 집계에 포함되지 않는다")
    void 다른_세션의_체크인은_제외된다() throws Exception {
        UUID targetSessionId = UUID.randomUUID();
        UUID otherSessionId = UUID.randomUUID();

        Reservation targetReservation = createReservation(targetSessionId);
        saveCheckedInTicket(targetReservation.getId(), AgeGroup.TWENTIES, Job.DEVELOPER);

        Reservation otherReservation = createReservation(otherSessionId);
        saveCheckedInTicket(otherReservation.getId(), AgeGroup.THIRTIES, Job.DESIGNER);

        mockMvc.perform(get("/internal/sessions/attendee-checkin-stats")
                        .param("sessionIds", targetSessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedInCount").value(1))
                .andExpect(jsonPath("$.data.ageGroupDistribution.THIRTIES").doesNotExist());
    }

    @Test
    @DisplayName("체크인 인원이 없으면 0명으로 응답한다")
    void 체크인_인원_없으면_0명() throws Exception {
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(get("/internal/sessions/attendee-checkin-stats")
                        .param("sessionIds", sessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedInCount").value(0));
    }

    private Reservation createReservation(UUID sessionId) {
        Reservation reservation = Reservation.builder()
                .sessionId(sessionId)
                .memberId(UUID.randomUUID())
                .headcount(1)
                .build();
        return reservationRepository.save(reservation);
    }

    private void saveCheckedInTicket(UUID reservationId, AgeGroup ageGroup, Job job) {
        QrTicket ticket = QrTicket.builder()
                .reservationId(reservationId)
                .code(UUID.randomUUID().toString())
                .ageGroup(ageGroup)
                .job(job)
                .build();
        ticket.markAsUsed();
        qrTicketRepository.save(ticket);
    }

    private void saveUncheckedTicket(UUID reservationId, AgeGroup ageGroup, Job job) {
        QrTicket ticket = QrTicket.builder()
                .reservationId(reservationId)
                .code(UUID.randomUUID().toString())
                .ageGroup(ageGroup)
                .job(job)
                .build();
        qrTicketRepository.save(ticket);
    }
}
