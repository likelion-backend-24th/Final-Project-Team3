package com.example.conferenceservice.attendeesummary.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AttendeeCheckinStatsClient {

    private final RestClient restClient;

    public AttendeeCheckinStatsClient(RestClient.Builder builder,
                                       @Value("${services.reservation-service.url}") String reservationServiceUrl) {
        this.restClient = builder
                .baseUrl(reservationServiceUrl)
                .build();
    }

    public AttendeeCheckinStatsResponse getAttendeeCheckinStats(List<UUID> sessionIds) {
        try {
            ApiResponseEnvelope<AttendeeCheckinStatsResponse> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/sessions/attendee-checkin-stats")
                            .queryParam("sessionIds", sessionIds)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new AttendeeStatsUnavailableException(sessionIds, null);
            }
            return response.data();
        } catch (RestClientException e) {
            throw new AttendeeStatsUnavailableException(sessionIds, e);
        }
    }

    public record ApiResponseEnvelope<T>(boolean success, T data, String message) {}

    public record AttendeeCheckinStatsResponse(
            int checkedInCount,
            Map<String, Long> ageGroupDistribution,
            Map<String, Long> jobDistribution
    ) {}
}
