package com.example.conferenceservice.attendeesummary.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

@Component
public class ReviewListClient {

    private final RestClient restClient;

    public ReviewListClient(RestClient.Builder builder,
                            @Value("${services.reservation-service.url}") String reservationServiceUrl) {
        this.restClient = builder
                .baseUrl(reservationServiceUrl)
                .build();
    }

    public List<String> getReviews(List<UUID> sessionIds) {
        try {
            ApiResponseEnvelope<ReviewListResponse> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/reservations/reviews")
                            .queryParam("sessionIds", sessionIds)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new AttendeeStatsUnavailableException(sessionIds, null);
            }
            return response.data().reviews();
        } catch (RestClientException e) {
            throw new AttendeeStatsUnavailableException(sessionIds, e);
        }
    }

    public record ApiResponseEnvelope<T>(boolean success, T data, String message) {}

    public record ReviewListResponse(List<String> reviews) {}
}
