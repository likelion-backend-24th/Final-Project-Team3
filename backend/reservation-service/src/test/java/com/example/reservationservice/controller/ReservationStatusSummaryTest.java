package com.example.reservationservice.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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

    private RequestPostProcessor asUser(UUID memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, MemberRole.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

    @Test
    @DisplayName("상태별 건수 합이 전체 신청 건수와 일치한다")
    void 상태별_건수_합이_전체와_일치한다() throws Exception {
        UUID sessionId = UUID.randomUUID();

        createReservation(sessionId, ReservationStatus.HOLD);
        createReservation(sessionId, ReservationStatus.QUEUED);
        createReservation(sessionId, ReservationStatus.CONFIRMED);
        createReservation(sessionId, ReservationStatus.CANCELLED);

        mockMvc.perform(get("/api/reservations/sessions/{sessionId}/status-summary", sessionId)
                        .with(asUser(UUID.randomUUID())))
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

        mockMvc.perform(get("/api/reservations/sessions/{sessionId}/status-summary", sessionId)
                        .with(asUser(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedInCount").value(1));
    }

    // Conference-Service(운영 현황)는 사용자 토큰 없이 호출하므로, 내부 경로는 인증 없이 열려 있어야 한다
    @Test
    @DisplayName("서비스 간 내부 경로는 인증 없이 상태별 건수를 조회할 수 있다")
    void 내부_경로는_인증_없이_조회된다() throws Exception {
        UUID sessionId = UUID.randomUUID();

        createReservation(sessionId, ReservationStatus.CONFIRMED);
        createReservation(sessionId, ReservationStatus.QUEUED);

        mockMvc.perform(get("/internal/sessions/{sessionId}/status-summary", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmedCount").value(1))
                .andExpect(jsonPath("$.data.queuedCount").value(1));
    }

    // 컨퍼런스 상세 화면은 로그인 안 한 방문자에게도 세션별 잔여석을 보여준다
    @Test
    @DisplayName("잔여석 조회는 인증 없이 가능하다")
    void 잔여석_조회는_인증_없이_가능하다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        mockMvc.perform(get("/api/reservations/sessions/{sessionId}/capacity-status", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(10))
                .andExpect(jsonPath("$.data.remaining").value(10));
    }

    // 잔여석만 공개하고, 상태별 집계(주최자 운영 현황용)는 계속 로그인이 필요하다
    @Test
    @DisplayName("상태별 집계는 인증 없이 조회할 수 없다")
    void 상태별_집계는_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/api/reservations/sessions/{sessionId}/status-summary", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
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