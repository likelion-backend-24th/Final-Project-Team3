package com.example.reservationservice.payment.controller;

import com.example.reservationservice.payment.service.PaymentWebhookService;
import io.portone.sdk.server.webhook.WebhookVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "결제", description = "PortOne 웹훅·결제 설정 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @Operation(summary = "PortOne 결제 웹훅 수신", description = "PortOne 서버가 결제 상태 변경 시 호출한다(프론트에서 호출하지 않음). "
            + "webhook-id/signature/timestamp 헤더의 서명을 검증한 뒤, payload의 paymentId(=reservationId)로 PortOne에 결제를 재조회해 좌석을 확정한다. "
            + "이미 확정된 건은 200으로 멱등 처리한다. 서명 오류 401, 헤더 누락 400, PG 미등록 또는 웹훅 시크릿 미설정 503")
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(WebhookVerifier.HEADER_ID) String webhookId,
            @RequestHeader(WebhookVerifier.HEADER_SIGNATURE) String webhookSignature,
            @RequestHeader(WebhookVerifier.HEADER_TIMESTAMP) String webhookTimestamp) {
        paymentWebhookService.handle(rawBody, webhookId, webhookSignature, webhookTimestamp);
        return ResponseEntity.ok().build();
    }
}
