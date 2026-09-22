package com.example.memberservice.member.verifier;

import com.example.memberservice.common.exception.BusinessException;
import com.example.memberservice.member.exception.MemberErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Duration;
import java.util.List;

// 국세청_사업자등록정보 진위확인 및 상태조회 서비스(공공데이터포털)의 상태조회 API 연동.
// nts.verify.mode=real 일 때만 등록된다.
@Slf4j
@Component
@ConditionalOnProperty(name = "nts.verify.mode", havingValue = "real")
public class NtsBusinessVerifier implements BusinessVerifier {

    private static final String STATUS_PATH = "/api/nts-businessman/v1/status";

    private final RestClient restClient;
    private final String baseUrl;
    private final String serviceKey;

    @Autowired
    public NtsBusinessVerifier(@Value("${nts.verify.base-url}") String baseUrl,
                               @Value("${nts.verify.service-key}") String serviceKey) {
        this(createRestClient(), baseUrl, serviceKey);
    }

    NtsBusinessVerifier(RestClient restClient, String baseUrl, String serviceKey) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("nts.verify.service-key(NTS_SERVICE_KEY)가 필요합니다. 로컬에서는 NTS_VERIFY_MODE=mock 으로 실행하세요.");
        }
        this.restClient = restClient;
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    private static RestClient createRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public BusinessStatus checkStatus(String businessNo) {
        StatusResponse response;
        try {
            response = restClient.post()
                    // 인증키는 포털이 주는 Encoding 키를 그대로 쓴다. URI 객체로 넘겨야 다시 인코딩되지 않는다.
                    .uri(URI.create(baseUrl + STATUS_PATH + "?serviceKey=" + serviceKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StatusRequest(List.of(businessNo)))
                    .retrieve()
                    .body(StatusResponse.class);
        } catch (RestClientException e) {
            // 예외 메시지에 serviceKey가 포함된 요청 URI가 들어가므로 클래스명만 남긴다
            log.error("국세청 상태조회 호출 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException(MemberErrorCode.BUSINESS_VERIFY_UNAVAILABLE);
        }

        if (response == null || response.data() == null || response.data().isEmpty()) {
            log.error("국세청 상태조회 응답 형식이 올바르지 않습니다.");
            throw new BusinessException(MemberErrorCode.BUSINESS_VERIFY_UNAVAILABLE);
        }

        String statusCode = response.data().get(0).statusCode();
        if (statusCode == null || statusCode.isBlank()) {
            return BusinessStatus.NOT_REGISTERED;
        }
        return switch (statusCode) {
            case "01" -> BusinessStatus.ACTIVE;
            case "02" -> BusinessStatus.SUSPENDED;
            case "03" -> BusinessStatus.CLOSED;
            default -> {
                log.error("국세청 상태조회 응답의 알 수 없는 상태 코드: {}", statusCode);
                throw new BusinessException(MemberErrorCode.BUSINESS_VERIFY_UNAVAILABLE);
            }
        };
    }

    record StatusRequest(@JsonProperty("b_no") List<String> businessNos) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StatusResponse(List<Item> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(@JsonProperty("b_stt_cd") String statusCode) {}
}
