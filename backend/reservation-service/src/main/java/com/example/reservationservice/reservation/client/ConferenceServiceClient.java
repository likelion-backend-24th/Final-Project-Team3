package com.example.reservationservice.reservation.client;

import com.example.reservationservice.reservation.exception.ConferenceServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class ConferenceServiceClient {

    private final RestClient restClient;

    // 예전엔 "http://localhost:8082"로 하드코딩돼 있어서, Docker 컨테이너 안에서는 localhost가
    // reservation-service 자기 자신(8083)을 가리켜 conference-service를 절대 못 찾았다(모든 hold 실패).
    // gateway와 같은 방식(services.conference-service.url)으로 환경변수화해서 compose 네트워크의
    // 서비스명(conference-service)을 주입받게 한다.
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

    // conference-service의 공통 ApiResponse<T> 래핑 규약에 맞춘 최소 파싱용 DTO
    public record ApiResponseEnvelope<T>(boolean success, T data, String message) {}

    public record SessionCapacityResponse(UUID sessionId, int capacity) {}
}
