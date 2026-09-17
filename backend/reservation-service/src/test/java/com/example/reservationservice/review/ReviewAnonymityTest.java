package com.example.reservationservice.review;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewAnonymityTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;
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
    }

    private RequestPostProcessor asUser(UUID memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, MemberRole.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

    @Test
    @DisplayName("후기 작성 응답에는 작성자 memberId·이름 등 식별 정보가 포함되지 않는다")
    void reviewResponse_doesNotExposeAuthorInfo() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        MvcResult holdResult = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId": "%s", "headcount": 1, "attendees": [{"ageGroup": "TWENTIES", "job": "DEVELOPER"}]}
                                """.formatted(sessionId)))
                .andReturn();
        String reservationId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.data.reservationId");

        mockMvc.perform(post("/api/reservations/{id}/payment", reservationId)
                .with(asUser(memberId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod": "CARD", "amount": 10000}
                        """));

        List<QrTicket> tickets = qrTicketRepository.findByReservationId(UUID.fromString(reservationId));
        QrTicket ticket = tickets.get(0);
        ticket.markAsUsed();
        qrTicketRepository.save(ticket);

        mockMvc.perform(post("/api/reservations/{id}/reviews", reservationId)
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "익명으로 남기는 후기"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").doesNotExist())
                .andExpect(jsonPath("$.data.name").doesNotExist())
                .andExpect(jsonPath("$.data.content").value("익명으로 남기는 후기"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.reservationId").value(reservationId));
    }
}