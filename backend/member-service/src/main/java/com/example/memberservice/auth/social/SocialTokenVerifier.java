package com.example.memberservice.auth.social;

import com.example.memberservice.auth.social.SocialProvider;

public interface SocialTokenVerifier {

    SocialProvider provider();

    // redirectUri는 Kakao(authorize 코드 교환)에서만 쓰이고 Google은 무시한다.
    VerifiedIdentity verify(String token, String redirectUri);

    record VerifiedIdentity(String providerUserId, String email, String name) {}
}
