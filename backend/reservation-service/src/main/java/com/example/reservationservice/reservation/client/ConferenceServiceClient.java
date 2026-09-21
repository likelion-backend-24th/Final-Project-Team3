package com.example.reservationservice.reservation.client;

import com.example.reservationservice.reservation.exception.ConferenceServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ConferenceServiceClient {

    private final RestClient restClient;

    public ConferenceServiceClient(RestClient.Builder builder, @Value("${services.conference-service.url}") String conferenceServiceUrl) {
        this.restClient = builder
                .baseUrl(conferenceServiceUrl)
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

    public Integer getSessionPrice(UUID sessionId) {
        try {
            ApiResponseEnvelope<SessionPriceResponse> response = restClient.get()
                    .uri("/api/sessions/{sessionId}/price", sessionId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new ConferenceServiceUnavailableException(sessionId, null);
            }
            return response.data().price();
        } catch (RestClientException e) {
            throw new ConferenceServiceUnavailableException(sessionId, e);
        }
    }

    public LocalDateTime getSessionStartAt(UUID sessionId) {
        try {
            ApiResponseEnvelope<SessionStartAtResponse> response = restClient.get()
                    .uri("/api/sessions/{sessionId}/startat", sessionId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new ConferenceServiceUnavailableException(sessionId, null);
            }
            return response.data().sessionStartAt();
        } catch (RestClientException e) {
            throw new ConferenceServiceUnavailableException(sessionId, e);
        }
    }

    public UUID getConferenceId(UUID sessionId) {
        try {
            ApiResponseEnvelope<SessionConferenceIdResponse> response = restClient.get()
                    .uri("/api/sessions/{sessionId}/conference-id", sessionId)                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new ConferenceServiceUnavailableException(sessionId, null);
            }
            return response.data().conferenceId();
        } catch (RestClientException e) {
            throw new ConferenceServiceUnavailableException(sessionId, e);
        }
    }

    public java.util.List<UUID> getSessionIdsByConference(UUID conferenceId) {
        try {
            ApiResponseEnvelope<java.util.List<UUID>> response = restClient.get()
                    .uri("/api/conferences/{conferenceId}/session-ids", conferenceId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new ConferenceServiceUnavailableException(conferenceId, null);
            }
            return response.data();
        } catch (RestClientException e) {
            throw new ConferenceServiceUnavailableException(conferenceId, e);
        }
    }

    public record SessionConferenceIdResponse(UUID sessionId, UUID conferenceId) {}

    // conference-service의 공통 ApiResponse<T> 래핑 규약에 맞춘 최소 파싱용 DTO
    public record ApiResponseEnvelope<T>(boolean success, T data, String message) {}

    public record SessionCapacityResponse(UUID sessionId, int capacity) {}

    /**
     * Conference-Service GET /api/sessions/{sessionId}/price 응답(내부 API).
     * price는 1인당 가격(원)이며 0이면 무료, null이면 가격 미정(무료로 취급하지 않고 결제 검증을 거친다).
     */
    public record SessionPriceResponse(UUID sessionId, Integer price) {}

    public record SessionStartAtResponse(UUID sessionId, LocalDateTime sessionStartAt) {}
}
