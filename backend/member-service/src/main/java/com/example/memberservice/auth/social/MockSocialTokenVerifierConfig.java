package com.example.memberservice.auth.social;

import com.example.memberservice.auth.entity.SocialProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "social.mode", havingValue = "mock")
public class MockSocialTokenVerifierConfig {

    @Bean
    public SocialTokenVerifier googleMockVerifier() {
        return new MockSocialTokenVerifier(SocialProvider.GOOGLE);
    }

    @Bean
    public SocialTokenVerifier kakaoMockVerifier() {
        return new MockSocialTokenVerifier(SocialProvider.KAKAO);
    }
}
