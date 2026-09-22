package com.example.reservationservice.reservation.client;

import com.example.reservationservice.reservation.exception.ConferenceServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
public class ConferenceServiceClient {

    private final RestClient restClient;

    public ConferenceServiceClient(RestClient.Builder builder, @Value("${services.conference-service.url}") String conferenceServiceUrl) {
        this.restClient = builder
                .baseUrl(conferenceServiceUrl)
                .build();
    }

    public int getSessionCapacity(UUID sessionId) {
        SessionCapacityResponse data = fetch(sessionId, "/api/sessions/{id}/capacity", new ParameterizedTypeReference<ApiResponseEnvelope<SessionCapacityResponse>>() {});
        return data.capacity();
    }

    public Integer getSessionPrice(UUID sessionId) {
        SessionPriceResponse data = fetch(sessionId, "/api/sessions/{id}/price", new ParameterizedTypeReference<ApiResponseEnvelope<SessionPriceResponse>>() {});
        return data.price();
    }

    public LocalDateTime getSessionStartAt(UUID sessionId) {
        SessionStartAtResponse data = fetch(sessionId, "/api/sessions/{id}/startat", new ParameterizedTypeReference<ApiResponseEnvelope<SessionStartAtResponse>>() {});
        return data.sessionStartAt();
    }

    @Cacheable(value = "conferenceId", key = "#sessionId")
    public UUID getConferenceId(UUID sessionId) {
        SessionConferenceIdResponse data = fetch(sessionId, "/api/sessions/{id}/conference-id", new ParameterizedTypeReference<ApiResponseEnvelope<SessionConferenceIdResponse>>() {});
        return data.conferenceId();
    }

    @Cacheable(value = "sessionIdsByConference", key = "#conferenceId")
    public List<UUID> getSessionIdsByConference(UUID conferenceId) {
        return fetch(conferenceId, "/api/conferences/{id}/session-ids", new ParameterizedTypeReference<ApiResponseEnvelope<List<UUID>>>() {});
    }

    // 5개 메서드가 공유하는 "요청 → 검증 → 데이터 추출" 패턴을 한 곳으로 모은 공통 헬퍼.
    // id는 URI 경로 변수이자, 실패 시 예외에 담을 식별자로 함께 쓰인다.
    private <T> T fetch(UUID id, String uriTemplate, ParameterizedTypeReference<ApiResponseEnvelope<T>> responseType) {
        try {
            ApiResponseEnvelope<T> response = restClient.get()
                    .uri(uriTemplate, id)
                    .retrieve()
                    .body(responseType);

            if (response == null || response.data() == null) {
                throw new ConferenceServiceUnavailableException(id, null);
            }
            return response.data();
        } catch (RestClientException e) {
            throw new ConferenceServiceUnavailableException(id, e);
        }
    }

    public record SessionConferenceIdResponse(UUID sessionId, UUID conferenceId) {}

    // conference-service의 공통 ApiResponse<T> 래핑 규약에 맞춘 최소 파싱용 DTO
    public record ApiResponseEnvelope<T>(boolean success, T data, String message) {}

    public record SessionCapacityResponse(UUID sessionId, int capacity) {}

    public record SessionPriceResponse(UUID sessionId, Integer price) {}

    public record SessionStartAtResponse(UUID sessionId, LocalDateTime sessionStartAt) {}
}