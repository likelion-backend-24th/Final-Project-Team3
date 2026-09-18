package com.example.reservationservice.payment.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.payment.repository.PaymentRepository;
import com.example.reservationservice.payment.service.PortOnePaymentVerifier;
import com.example.reservationservice.pgcredential.PgCredentialEncryptor;
import com.example.reservationservice.pgcredential.entity.PgCredential;
import com.example.reservationservice.pgcredential.repository.PgCredentialRepository;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import com.jayway.jsonpath.JsonPath;
import io.portone.sdk.server.webhook.WebhookVerifier;
import org.junit.jupiter.api.AfterEach;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentWebhookAcceptanceTest {

    private static final String WEBHOOK_SECRET_PLAIN = Base64.getEncoder().encodeToString("test-webhook-secret".getBytes(StandardCharsets.UTF_8));

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private WaitingQueueRepository waitingQueueRepository;
    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PgCredentialRepository pgCredentialRepository;
    @Autowired
    private PgCredentialEncryptor pgCredentialEncryptor;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;
    @MockitoBean
    private PortOnePaymentVerifier portOnePaymentVerifier;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        waitingQueueRepository.deleteAll();
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
        pgCredentialRepository.deleteAll();

        pgCredentialRepository.save(PgCredential.builder()
                .provider("PORTONE")
                .storeId("store-test")
                .channelKey("channel-test")
                .apiSecret(pgCredentialEncryptor.encrypt("api-secret-test"))
                .webhookSecret(pgCredentialEncryptor.encrypt(WEBHOOK_SECRET_PLAIN))
                .build());

        given(portOnePaymentVerifier.verify(anyString(), anyInt()))
                .willReturn(new PortOnePaymentVerifier.VerifiedPayment("CARD"));
    }

    @AfterEach
    void tearDown() {
        pgCredentialRepository.deleteAll();
    }

    private RequestPostProcessor asUser(UUID memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, MemberRole.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

    private String createHold(UUID sessionId, UUID memberId) throws Exception {
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        MvcResult holdResult = mockMvc.perform(post("/api/reservations/hold")
                        .with(asUser(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId": "%s", "headcount": 1, "attendees": [{"ageGroup": "TWENTIES", "job": "DEVELOPER"}]}
                                """.formatted(sessionId)))
                .andReturn();
        return JsonPath.read(holdResult.getResponse().getContentAsString(), "$.data.reservationId");
    }

    private String sign(String msgId, String timestamp, String payload) throws Exception {
        byte[] key = Base64.getDecoder().decode(WEBHOOK_SECRET_PLAIN);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        byte[] signature = mac.doFinal((msgId + "." + timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "v1," + Base64.getEncoder().encodeToString(signature);
    }

    private String webhookPayload(String reservationId) {
        return """
                {"type":"Transaction.Paid","timestamp":"2026-09-17T10:00:00.000Z","data":{"paymentId":"%s","storeId":"store-test","transactionId":"txn-1"}}
                """.formatted(reservationId).strip();
    }

    @Test
    @DisplayName("서명이 유효한 결제완료 웹훅을 받으면 예약이 확정된다")
    void validWebhook_confirmsReservation() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String reservationId = createHold(sessionId, UUID.randomUUID());
        String payload = webhookPayload(reservationId);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookVerifier.HEADER_ID, "msg-1")
                        .header(WebhookVerifier.HEADER_TIMESTAMP, timestamp)
                        .header(WebhookVerifier.HEADER_SIGNATURE, sign("msg-1", timestamp, payload))
                        .content(payload))
                .andExpect(status().isOk());

        Reservation reservation = reservationRepository.findById(UUID.fromString(reservationId)).orElseThrow();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("서명이 위조된 웹훅은 401로 거부되고 예약은 확정되지 않는다")
    void invalidSignature_rejectedAndReservationUntouched() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String reservationId = createHold(sessionId, UUID.randomUUID());
        String payload = webhookPayload(reservationId);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookVerifier.HEADER_ID, "msg-1")
                        .header(WebhookVerifier.HEADER_TIMESTAMP, timestamp)
                        .header(WebhookVerifier.HEADER_SIGNATURE, "v1,forged-signature")
                        .content(payload))
                .andExpect(status().isUnauthorized());

        Reservation reservation = reservationRepository.findById(UUID.fromString(reservationId)).orElseThrow();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HOLD);
    }

    @Test
    @DisplayName("서명 헤더가 없으면 400을 반환한다")
    void missingHeaders_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload(UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 확정된 예약에 대한 중복 웹훅은 200을 반환하고 결제가 중복 저장되지 않는다")
    void duplicateWebhook_isIdempotent() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String reservationId = createHold(sessionId, UUID.randomUUID());
        String payload = webhookPayload(reservationId);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = sign("msg-1", timestamp, payload);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookVerifier.HEADER_ID, "msg-1")
                        .header(WebhookVerifier.HEADER_TIMESTAMP, timestamp)
                        .header(WebhookVerifier.HEADER_SIGNATURE, signature)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookVerifier.HEADER_ID, "msg-1")
                        .header(WebhookVerifier.HEADER_TIMESTAMP, timestamp)
                        .header(WebhookVerifier.HEADER_SIGNATURE, signature)
                        .content(payload))
                .andExpect(status().isOk());

        assertThat(paymentRepository.findByReservationId(UUID.fromString(reservationId))).isPresent();
        assertThat(paymentRepository.count()).isEqualTo(1);
    }
}
