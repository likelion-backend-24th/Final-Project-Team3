package com.example.memberservice.auth.social;

import com.example.memberservice.auth.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 로컬·테스트 전용. 실제 Google/Kakao 서버를 안 부르고 token을 "email:name" 문자열로 취급한다.
// MockSocialTokenVerifierConfig가 provider별로 인스턴스를 만들어 등록하므로 여기엔 @Component를 붙이지 않는다.
@Slf4j
@RequiredArgsConstructor
public class MockSocialTokenVerifier implements SocialTokenVerifier {

    private final SocialProvider provider;

    @Override
    public SocialProvider provider() {
        return provider;
    }

    @Override
    public VerifiedIdentity verify(String token, String redirectUri) {
        log.warn("[MOCK] {} 소셜 로그인 검증을 건너뜁니다: {}", provider, token);
        String[] parts = token.split(":", 2);
        String email = parts[0];
        String name = parts.length > 1 ? parts[1] : email;
        return new VerifiedIdentity("mock-" + provider + "-" + email, email, name);
    }
}
