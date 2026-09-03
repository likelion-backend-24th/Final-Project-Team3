package com.example.reservationservice.reservation.client;

import com.example.reservationservice.reservation.exception.ConferenceServiceUnavailableException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class ConferenceServiceClient {

    private final RestClient restClient;

    public ConferenceServiceClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://localhost:8082")
                .build();
    }

    public int getSessionCapacity(UUID sessionId) {
        try {
            ApiResponseEnvelope<SessionCapacityResponse> response = restClient.get()
                    .uri("/api/sessions/{sessionId}/capacity", sessionId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new ConferenceServiceUnavailableException(sessionId, null);
            }
            return response.data().capacity();
        } catch (RestClientException e) {
            throw new ConferenceServiceUnavailableException(sessionId, e);
        }
    }

    // conference-service의 공통 ApiResponse<T> 래핑 규약에 맞춘 최소 파싱용 DTO
    public record ApiResponseEnvelope<T>(boolean success, T data, String message) {}

    public record SessionCapacityResponse(UUID sessionId, int capacity) {}
}
