package com.example.reservationservice.review;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.payment.service.PortOnePaymentVerifier;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import com.example.reservationservice.review.repository.ReviewRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;
    @MockitoBean
    private PortOnePaymentVerifier portOnePaymentVerifier;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private WaitingQueueRepository waitingQueueRepository;
    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;
    @Autowired
    private QrTicketRepository qrTicketRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        qrTicketRepository.deleteAll();
        waitingQueueRepository.deleteAll();
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
        given(portOnePaymentVerifier.verify(anyString(), anyInt()))
                .willReturn(new PortOnePaymentVerifier.VerifiedPayment("CARD"));
    }

    private RequestPostProcessor asUser(UUID memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, MemberRole.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

    @Test
    @DisplayName("체크인 완료된 예약은 후기를 작성할 수 있고, 응답에 저장한 내용이 그대로 반환된다")
    void writeReview_afterCheckIn_succeeds() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        String reservationId = holdAndPay(sessionId, memberId);
        markFirstTicketAsUsed(reservationId);

        mockMvc.perform(post("/api/reservations/{id}/reviews", reservationId)
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "좋은 세션이었어요"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("좋은 세션이었어요"))
                .andExpect(jsonPath("$.data.reservationId").value(reservationId));
    }

    @Test
    @DisplayName("체크인 완료 좌석이 없으면 후기 작성이 403으로 거부된다")
    void writeReview_withoutCheckIn_rejected() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        String reservationId = holdAndPay(sessionId, memberId);
        // markFirstTicketAsUsed 호출 안 함 -> 체크인 안 된 상태

        mockMvc.perform(post("/api/reservations/{id}/reviews", reservationId)
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "후기"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_ELIGIBLE"));
    }

    @Test
    @DisplayName("존재하지 않는 예약으로 작성 요청하면 404로 거부된다")
    void writeReview_reservationNotFound_rejected() throws Exception {
        mockMvc.perform(post("/api/reservations/{id}/reviews", UUID.randomUUID())
                .with(asUser(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "후기"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REVIEW_RESERVATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("본인 예약이 아니면 403으로 거부된다")
    void writeReview_notOwner_rejected() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        String reservationId = holdAndPay(sessionId, ownerId);
        markFirstTicketAsUsed(reservationId);

        mockMvc.perform(post("/api/reservations/{id}/reviews", reservationId)
                        .with(asUser(strangerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "후기"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_OWNER"));
    }

    @Test
    @DisplayName("이미 후기가 있는 예약에 다시 작성 요청하면 새로 생기지 않고 내용만 수정된다")
    void writeReview_twice_updatesInPlace() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        String reservationId = holdAndPay(sessionId, memberId);
        markFirstTicketAsUsed(reservationId);

        mockMvc.perform(post("/api/reservations/{id}/reviews", reservationId)
                .with(asUser(memberId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"content": "처음 작성한 후기"}
                        """));

        mockMvc.perform(post("/api/reservations/{id}/reviews", reservationId)
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "수정한 후기"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정한 후기"));

        assertThat(reviewRepository.count()).isEqualTo(1);
    }

    private String holdAndPay(UUID sessionId, UUID memberId) throws Exception {
        MvcResult holdResult = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(memberId))  // ← 추가
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId": "%s", "headcount": 1, "attendees": [{"ageGroup": "TWENTIES", "job": "DEVELOPER"}]}
                                """.formatted(sessionId)))
                .andReturn();
        String reservationId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(post("/api/reservations/{id}/payment", reservationId)
                .with(asUser(memberId))  // ← 추가
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentId": "test-payment-id"}
                        """));

        return reservationId;
    }

    private void markFirstTicketAsUsed(String reservationId) {
        List<QrTicket> tickets = qrTicketRepository.findByReservationId(UUID.fromString(reservationId));
        QrTicket ticket = tickets.get(0);
        ticket.markAsUsed();
        qrTicketRepository.save(ticket);
    }
}
