package com.example.reservationservice.payment.controller;

import com.example.reservationservice.payment.service.PaymentWebhookService;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

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
