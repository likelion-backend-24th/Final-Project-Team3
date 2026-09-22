package com.example.memberservice.auth.social;

import com.example.memberservice.auth.entity.SocialProvider;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.common.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

// 프론트가 Kakao SDK로 받은 accessToken을 그대로 받아 카카오 사용자 정보 API로 검증한다.
// accessToken 자체가 카카오 서버에 대한 신원증명이라 백엔드에 별도 앱 키가 필요 없다.
// social.mode=real 일 때만 등록된다.
@Slf4j
@Component
@ConditionalOnProperty(name = "social.mode", havingValue = "real")
public class KakaoSocialTokenVerifier implements SocialTokenVerifier {

    private static final String USER_ME_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;

    public KakaoSocialTokenVerifier() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public VerifiedIdentity verify(String accessToken) {
        KakaoUserResponse response;
        try {
            response = restClient.get()
                    .uri(USER_ME_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);
        } catch (RestClientException e) {
            log.error("Kakao 사용자 정보 조회 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }

        if (response == null || response.kakaoAccount() == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }
        // 카카오는 이메일 제공에 사용자가 별도 동의해야 내려온다(앱이 이메일 항목 비즈 심사 필요).
        String email = response.kakaoAccount().email();
        if (email == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_EMAIL_REQUIRED);
        }

        String name = response.kakaoAccount().profile() != null ? response.kakaoAccount().profile().nickname() : email;
        return new VerifiedIdentity(String.valueOf(response.id()), email, name);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoUserResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoAccount(String email, Profile profile) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Profile(String nickname) {}
}
