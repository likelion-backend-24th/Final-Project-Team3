package com.example.reservationservice.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class QrTicketScanAcceptanceTest {

    private static final UUID ORGANIZER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private QrTicketRepository qrTicketRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("유효한 QR 티켓은 세션 시작 이후 정상적으로 스캔되어 입장 처리된다")
    void 정상_스캔() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Reservation reservation = createReservation(sessionId);

        given(conferenceServiceClient.getOrganizerId(sessionId))
                .willReturn(ORGANIZER_ID);
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().minusHours(1));

        QrTicket ticket = createQrTicket(reservation.getId(), "VALID-CODE-1");
        qrTicketRepository.save(ticket);

        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "VALID-CODE-1")
                        .with(asOrganizer()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.used").value(true));
    }

    @Test
    @DisplayName("존재하지 않는 코드는 404로 거절한다")
    void 존재하지않는_코드_404() throws Exception {
        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "NOT-EXIST")
                        .with(asOrganizer()))
                .andDo(print())
                .andExpect(status().isNotFound())
                // 로그 확인 결과, 커스텀 에러는 $.error 하위에 매핑됨
                .andExpect(jsonPath("$.error.code").value("QR_TICKET_NOT_FOUND"));
    }

    @Test
    @DisplayName("주최자 권한이 없는 사용자가 스캔하면 403으로 거절한다")
    void 권한없는_사용자_스캔_거절_403() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Reservation reservation = createReservation(sessionId);

        given(conferenceServiceClient.getOrganizerId(sessionId))
                .willReturn(UUID.randomUUID()); // 다른 주최자 ID 반환

        QrTicket ticket = createQrTicket(reservation.getId(), "VALID-CODE-2");
        qrTicketRepository.save(ticket);

        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "VALID-CODE-2")
                        .with(asOrganizer()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("QR_TICKET_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("세션(행사) 시작 전에는 스캔이 거절된다")
    void 세션_시작전_스캔_거절_403() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Reservation reservation = createReservation(sessionId);

        given(conferenceServiceClient.getOrganizerId(sessionId))
                .willReturn(ORGANIZER_ID);
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().plusHours(1)); // 미래 시간

        QrTicket ticket = createQrTicket(reservation.getId(), "FUTURE-CODE");
        qrTicketRepository.save(ticket);

        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "FUTURE-CODE")
                        .with(asOrganizer()))
                .andDo(print())
                .andExpect(status().isForbidden()) // 403 확인
                .andExpect(jsonPath("$.error.code").value("QR_TICKET_SESSION_NOT_STARTED")); // 에러 코드 확인
    }
    
    @Test
    @DisplayName("이미 사용된 코드는 409로 거절한다")
    void 이미사용된_코드_409() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Reservation reservation = createReservation(sessionId);

        given(conferenceServiceClient.getOrganizerId(sessionId))
                .willReturn(ORGANIZER_ID);
        given(conferenceServiceClient.getSessionStartAt(sessionId))
                .willReturn(LocalDateTime.now().minusHours(1));

        QrTicket ticket = createQrTicket(reservation.getId(), "USED-CODE");
        ticket.scan();
        qrTicketRepository.saveAndFlush(ticket);

        mockMvc.perform(post("/api/qr-tickets/{code}/scan", "USED-CODE")
                        .with(asOrganizer()))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("QR_TICKET_ALREADY_USED"));
    }

    private RequestPostProcessor asOrganizer() {
        CustomUserDetails userDetails = new CustomUserDetails(
                ORGANIZER_ID,
                MemberRole.ORGANIZER
        );
        return user(userDetails);
    }

    private Reservation createReservation(UUID sessionId) {
        Reservation reservation = Reservation.builder()
                .sessionId(sessionId)
                .memberId(ORGANIZER_ID)
                // [수정됨] NULL not allowed for column "headcount" 에러 해결
                .headcount(1)
                // 만약 status나 expiresAt 등의 필수 필드가 있다면 아래처럼 채워주세요
                // .status(ReservationStatus.CONFIRMED)
                // .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        return reservationRepository.save(reservation);
    }

    private QrTicket createQrTicket(UUID reservationId, String code) {
        return QrTicket.builder()
                .reservationId(reservationId)
                .code(code)
                .ageGroup(AgeGroup.TWENTIES)
                .job(Job.STUDENT)
                .build();
    }
}