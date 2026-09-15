package com.example.conferenceservice.operationstatus.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class ReservationServiceClient {

    private final RestClient restClient;

    public ReservationServiceClient(RestClient.Builder builder,
                                     @Value("${services.reservation-service.url}") String reservationServiceUrl) {
        this.restClient = builder
                .baseUrl(reservationServiceUrl)
                .build();
    }

    public SessionStatusSummaryResponse getStatusSummary(UUID sessionId) {
        try {
            ApiResponseEnvelope<SessionStatusSummaryResponse> response = restClient.get()
                    .uri("/api/reservations/sessions/{sessionId}/status-summary", sessionId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new ReservationServiceUnavailableException(sessionId, null);
            }
            return response.data();
        } catch (RestClientException e) {
            throw new ReservationServiceUnavailableException(sessionId, e);
        }
    }

    // reservation-service의 공통 ApiResponse<T> 래핑 규약에 맞춘 최소 파싱용 DTO
    public record ApiResponseEnvelope<T>(boolean success, T data, String message) {}

    public record SessionStatusSummaryResponse(
            UUID sessionId,
            long holdCount,
            long queuedCount,
            long confirmedCount,
            long cancelledCount,
            long checkedInCount
    ) {}
}
