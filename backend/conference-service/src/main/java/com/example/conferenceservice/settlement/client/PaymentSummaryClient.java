package com.example.conferenceservice.settlement.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

@Component
public class PaymentSummaryClient {

    private final RestClient restClient;

    public PaymentSummaryClient(RestClient.Builder builder,
                                 @Value("${services.reservation-service.url}") String reservationServiceUrl) {
        this.restClient = builder
                .baseUrl(reservationServiceUrl)
                .build();
    }

    public PaymentSummaryResponse getPaymentSummary(List<UUID> sessionIds) {
        try {
            ApiResponseEnvelope<PaymentSummaryResponse> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/sessions/payment-summary")
                            .queryParam("sessionIds", sessionIds)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new PaymentSummaryUnavailableException(sessionIds, null);
            }
            return response.data();
        } catch (RestClientException e) {
            throw new PaymentSummaryUnavailableException(sessionIds, e);
        }
    }

    public record ApiResponseEnvelope<T>(boolean success, T data, String message) {}

    public record PaymentSummaryResponse(
            int totalRevenue,
            int refundedAmount,
            int netRevenue,
            int confirmedCount,
            int cancelledCount
    ) {}
}
