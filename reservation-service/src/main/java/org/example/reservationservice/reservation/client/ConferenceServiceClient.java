package org.example.reservationservice.reservation.client;

import org.example.reservationservice.reservation.exception.ConferenceServiceUnavailableException;
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
            CapacityResponse response = restClient.get()
                    .uri("/api/confences/{id}/capacity", sessionId)
                    .retrieve()
                    .body(CapacityResponse.class);

            if (response == null) {
                throw new ConferenceServiceUnavailableException(sessionId, null);
            }
            return response.capacity();
        } catch (RestClientException e) {
            throw new ConferenceServiceUnavailableException(sessionId, e);
        }
    }

    // TODO: 기혁님 API 실제 응답 필드명에 맞춰 조정 필요
    public record CapacityResponse(int capacity) {}
}
