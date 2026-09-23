package com.example.memberservice.member.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.UUID;

@Component
public class ConferenceServiceClient {

    private final RestClient restClient;

    public ConferenceServiceClient(@Value("${services.conference-service.url}") String conferenceServiceUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(conferenceServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean hasActiveConference(UUID organizerId) {
        try {
            ApiResponseEnvelope<WithdrawalEligibilityResponse> response = restClient.get()
                    .uri("/api/organizers/{id}/withdrawal-eligibility", organizerId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponseEnvelope<WithdrawalEligibilityResponse>>() {});

            if (response == null || response.data() == null) {
                throw new ConferenceServiceUnavailableException(organizerId, null);
            }
            return response.data().hasActiveConference();
        } catch (RestClientException e) {
            throw new ConferenceServiceUnavailableException(organizerId, e);
        }
    }

    // conference-service의 공통 ApiResponse<T> 래핑 규약에 맞춘 최소 파싱용 DTO
    public record ApiResponseEnvelope<T>(boolean success, T data, String message) {}

    public record WithdrawalEligibilityResponse(UUID organizerId, boolean hasActiveConference) {}
}