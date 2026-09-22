package com.example.memberservice.auth.social;

import com.example.memberservice.auth.entity.SocialProvider;

public interface SocialTokenVerifier {

    SocialProvider provider();

    VerifiedIdentity verify(String idTokenString);

    record VerifiedIdentity(String providerUserId, String email, String name) {}
}
