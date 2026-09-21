package com.example.reservationservice.payment.service;

import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.pgcredential.PgCredentialEncryptor;
import com.example.reservationservice.pgcredential.entity.PgCredential;
import com.example.reservationservice.pgcredential.repository.PgCredentialRepository;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import com.example.reservationservice.reservation.service.ReservationService;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransaction;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

// PortOne 웹훅 본문 자체는 신뢰하지 않는다 — data.paymentId만 신호로 쓰고, 실제 결제 여부·금액은
// 항상 PortOnePaymentVerifier가 PortOne 서버에 재조회해서 검증한다(processPayment와 동일 경로).
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private static final String PROVIDER = "PORTONE";

    private final PgCredentialRepository pgCredentialRepository;
    private final PgCredentialEncryptor encryptor;
    private final ReservationService reservationService;

    public void handle(String rawBody, String webhookId, String webhookSignature, String webhookTimestamp) {
        String webhookSecret = decryptWebhookSecret();
        // 웹훅 시크릿 미발급 상태(env 등록 시 빈 값 허용)에서는 검증할 수 없으므로 503 — PortOne이 재시도한다
        if (webhookSecret.isBlank()) {
            throw new BusinessException(ReservationErrorCode.PG_NOT_CONFIGURED);
        }
        WebhookVerifier verifier = new WebhookVerifier(webhookSecret);

        Webhook webhook;
        try {
            webhook = verifier.verify(rawBody, webhookId, webhookSignature, webhookTimestamp);
        } catch (WebhookVerificationException e) {
            throw new BusinessException(ReservationErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }

        if (!(webhook instanceof WebhookTransaction transaction)) {
            return;
        }

        // paymentId는 결제 시작 시 예약 확정 API와 동일한 값(reservationId 문자열)을 사용한다.
        String paymentId = transaction.getData().getPaymentId();
        UUID reservationId;
        try {
            reservationId = UUID.fromString(paymentId);
        } catch (IllegalArgumentException e) {
            log.warn("웹훅 paymentId가 UUID 형식이 아님: {}", paymentId);
            return;
        }

        reservationService.confirmFromWebhook(reservationId, paymentId);
    }

    private String decryptWebhookSecret() {
        PgCredential credential = pgCredentialRepository.findByProvider(PROVIDER)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.PG_NOT_CONFIGURED));
        return encryptor.decrypt(credential.getWebhookSecret());
    }
}
