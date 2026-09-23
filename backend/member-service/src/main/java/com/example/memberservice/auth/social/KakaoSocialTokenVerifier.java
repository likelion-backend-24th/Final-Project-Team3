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
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

// Kakao JS SDK v2는 팝업 로그인(Auth.login)을 지원하지 않고 Auth.authorize()로 인가 코드를 받는 방식만 지원한다.
// 그래서 프론트가 넘겨준 인가 코드를 REST API 키로 액세스 토큰과 교환한 다음, 그 토큰으로 사용자 정보를 조회한다.
// social.mode=real 일 때만 등록된다.
@Slf4j
@Component
@ConditionalOnProperty(name = "social.mode", havingValue = "real")
public class KakaoSocialTokenVerifier implements SocialTokenVerifier {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_ME_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final String restApiKey;
    private final String clientSecret;

    public KakaoSocialTokenVerifier(
            @Value("${social.kakao.rest-api-key}") String restApiKey,
            @Value("${social.kakao.client-secret:}") String clientSecret
    ) {
        if (restApiKey == null || restApiKey.isBlank()) {
            throw new IllegalStateException("social.kakao.rest-api-key(KAKAO_REST_API_KEY)가 필요합니다. 로컬에서는 SOCIAL_MODE=mock 으로 실행하세요.");
        }
        this.restApiKey = restApiKey;
        this.clientSecret = clientSecret;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        // User-Agent가 없으면(Java 기본값) Kakao 쪽에서 봇 트래픽으로 보고 실제 에러 대신
        // 빈 401("Username/Password Authentication Failed.")을 돌려준다 — 브라우저처럼 보이게 명시적으로 지정
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public VerifiedIdentity verify(String code, String redirectUri) {
        String accessToken = exchangeCodeForAccessToken(code, redirectUri);
        return fetchIdentity(accessToken);
    }

    private String exchangeCodeForAccessToken(String code, String redirectUri) {
        StringBuilder body = new StringBuilder("grant_type=authorization_code")
                .append("&client_id=").append(UriUtils.encode(restApiKey, StandardCharsets.UTF_8))
                .append("&redirect_uri=").append(UriUtils.encode(redirectUri, StandardCharsets.UTF_8))
                .append("&code=").append(UriUtils.encode(code, StandardCharsets.UTF_8));
        if (clientSecret != null && !clientSecret.isBlank()) {
            body.append("&client_secret=").append(UriUtils.encode(clientSecret, StandardCharsets.UTF_8));
        }

        TokenResponse response;
        try {
            response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body.toString())
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Kakao 토큰 교환 실패: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        } catch (RestClientException e) {
            log.error("Kakao 토큰 교환 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }

        if (response == null || response.accessToken() == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }
        return response.accessToken();
    }

    private VerifiedIdentity fetchIdentity(String accessToken) {
        KakaoUserResponse response;
        try {
            response = restClient.get()
                    .uri(USER_ME_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Kakao 사용자 정보 조회 실패: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        } catch (RestClientException e) {
            log.error("Kakao 사용자 정보 조회 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }

        if (response == null || response.id() == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }

        // 비즈 앱 전환 전에는 이메일 동의항목 자체를 신청할 수 없어 kakao_account가 아예 비거나
        // email이 없을 수 있다 — 그 경우 카카오 회원번호 기반 가짜 이메일로 대체한다.
        // 이 계정은 이메일 알림·비밀번호 찾기 등 이메일이 필요한 기능은 쓸 수 없다.
        String email = response.kakaoAccount() != null ? response.kakaoAccount().email() : null;
        if (email == null) {
            email = "kakao_" + response.id() + "@kakao.local";
        }

        String nickname = response.kakaoAccount() != null && response.kakaoAccount().profile() != null
                ? response.kakaoAccount().profile().nickname()
                : null;
        String name = nickname != null ? nickname : "카카오사용자";

        return new VerifiedIdentity(String.valueOf(response.id()), email, name);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoUserResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoAccount(String email, Profile profile) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Profile(String nickname) {}
}
