package com.example.memberservice.auth.social;

import com.example.memberservice.auth.entity.SocialProvider;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.common.exception.BusinessException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
@ConditionalOnProperty(name = "social.google.mode", havingValue = "real")
public class GoogleSocialTokenVerifier implements SocialTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleSocialTokenVerifier(@Value("${social.google.client-id}") String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("social.google.client-id(GOOGLE_OAUTH_CLIENT_ID)가 필요합니다. 로컬에서는 SOCIAL_MODE=mock 으로 실행하세요.");
        }
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public VerifiedIdentity verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            if (email == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
            }
            String name = (String) payload.get("name");
            return new VerifiedIdentity(payload.getSubject(), email, name != null ? name : email);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google ID Token 검증 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }
    }
}
