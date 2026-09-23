package com.example.memberservice.auth.social;

import com.example.memberservice.auth.social.SocialProvider;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.common.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

// Google Identity Services의 OAuth2 팝업(initTokenClient)으로 받은 access token을 그대로 받아
// Google 사용자 정보 API로 검증한다. 예전 ID Token(One Tap) 방식은 "이미 구글에 로그인된 세션"이
// 있어야만 동작해서, 로그인 안 된 사용자(시크릿창 등)는 버튼을 눌러도 아무 반응이 없는 문제가 있었다.
// access token은 어느 클라이언트가 발급받은 건지 audience 검증이 없으면 다른 앱에서 발급받은
// 토큰을 재사용해 남의 이메일로 로그인할 수 있어서, tokeninfo로 aud(client_id 일치)까지 확인한다.
// social.mode=real 일 때만 등록된다.
@Slf4j
@Component
@ConditionalOnProperty(name = "social.mode", havingValue = "real")
public class GoogleSocialTokenVerifier implements SocialTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient;
    private final String clientId;

    public GoogleSocialTokenVerifier(@Value("${social.google.client-id}") String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("social.google.client-id(GOOGLE_OAUTH_CLIENT_ID)가 필요합니다. 로컬에서는 SOCIAL_MODE=mock 으로 실행하세요.");
        }
        this.clientId = clientId;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public VerifiedIdentity verify(String accessToken, String redirectUri) {
        verifyAudience(accessToken);
        return fetchIdentity(accessToken);
    }

    private void verifyAudience(String accessToken) {
        TokenInfoResponse info;
        try {
            String uri = UriComponentsBuilder.fromUriString(TOKENINFO_URL)
                    .queryParam("access_token", accessToken)
                    .toUriString();
            info = restClient.get().uri(uri).retrieve().body(TokenInfoResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Google 토큰 검증 실패: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        } catch (RestClientException e) {
            log.error("Google 토큰 검증 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }

        if (info == null || !clientId.equals(info.aud())) {
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }
    }

    private VerifiedIdentity fetchIdentity(String accessToken) {
        GoogleUserResponse response;
        try {
            response = restClient.get()
                    .uri(USERINFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Google 사용자 정보 조회 실패: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        } catch (RestClientException e) {
            log.error("Google 사용자 정보 조회 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }

        if (response == null || response.sub() == null || response.email() == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }
        if (!Boolean.TRUE.equals(response.emailVerified())) {
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }

        String name = response.name() != null ? response.name() : response.email();
        return new VerifiedIdentity(response.sub(), response.email(), name);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenInfoResponse(String aud) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GoogleUserResponse(
            String sub,
            String email,
            @JsonProperty("email_verified") Boolean emailVerified,
            String name
    ) {}
}
