package com.example.memberservice.auth.service;

import com.example.memberservice.auth.dto.LinkedSocialAccountResponse;
import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.auth.dto.LoginResponse;
import com.example.memberservice.auth.dto.SocialLoginRequest;
import com.example.memberservice.auth.entity.SocialAccount;
import com.example.memberservice.auth.entity.SocialProvider;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.repository.SocialAccountRepository;
import com.example.memberservice.auth.security.JwtTokenProvider;
import com.example.memberservice.auth.social.SocialTokenVerifier;
import com.example.memberservice.common.exception.BusinessException;
import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.Role;
import com.example.memberservice.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final SocialAccountRepository socialAccountRepository;
    private final List<SocialTokenVerifier> socialTokenVerifiers;

    @Transactional
    public AuthTokens login(LoginRequest request) {
        Member member = memberRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (member.getPassword() == null || !passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(member);
    }

    @Transactional
    public AuthTokens socialLogin(SocialProvider provider, String token, String redirectUri, AgeGroup ageGroup, Job job) {
        SocialTokenVerifier verifier = socialTokenVerifiers.stream()
                .filter(v -> v.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new BusinessException(AuthErrorCode.SOCIAL_PROVIDER_UNSUPPORTED));

        SocialTokenVerifier.VerifiedIdentity identity = verifier.verify(token, redirectUri);

        Member member = socialAccountRepository.findByProviderAndProviderUserId(provider, identity.providerUserId())
                .map(SocialAccount::getMember)
                .orElseGet(() -> registerSocialMember(provider, identity, ageGroup, job));

        return issueTokens(member);
    }

    private Member registerSocialMember(SocialProvider provider, SocialTokenVerifier.VerifiedIdentity identity, AgeGroup ageGroup, Job job) {
        String email = normalize(identity.email());
        memberRepository.findByEmail(email).ifPresent(existing -> {
            // 주최자·관리자는 애초에 소셜 로그인 대상이 아니라서 "로그인 후 연동" 안내 자체가 성립 안 함
            // (주최자는 참석자 전용인 /mypage 계정 연동 화면에 접근할 수 없음)
            if (existing.getRole() != Role.MEMBER) {
                throw new BusinessException(AuthErrorCode.SOCIAL_EMAIL_NOT_LINKABLE);
            }
            throw new BusinessException(AuthErrorCode.SOCIAL_EMAIL_ALREADY_REGISTERED);
        });
        // 최초 가입일 때만 필요 — 재로그인 경로(위 findByProviderAndProviderUserId 매칭)는 여기 안 탐
        if (ageGroup == null || job == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_PROFILE_REQUIRED);
        }

        Member member = Member.newSocialMember(email, identity.name(), ageGroup, job);
        memberRepository.save(member);
        socialAccountRepository.save(SocialAccount.of(member, provider, identity.providerUserId()));
        return member;
    }

    @Transactional
    public void linkSocialAccount(UUID memberId, SocialProvider provider, String token, String redirectUri) {
        SocialTokenVerifier verifier = socialTokenVerifiers.stream()
                .filter(v -> v.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new BusinessException(AuthErrorCode.SOCIAL_PROVIDER_UNSUPPORTED));

        SocialTokenVerifier.VerifiedIdentity identity = verifier.verify(token, redirectUri);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        // 본인 확인의 핵심 — 소셜 인증으로 나온 이메일이 "지금 로그인한 계정"의 이메일과 같아야만 연동을 허용한다.
        // 다른 이메일의 소셜 계정을 아무거나 붙일 수 있게 하면 이 검증 자체가 무의미해짐
        if (!normalize(identity.email()).equals(member.getEmail())) {
            throw new BusinessException(AuthErrorCode.SOCIAL_LINK_EMAIL_MISMATCH);
        }

        if (socialAccountRepository.findByProviderAndProviderUserId(provider, identity.providerUserId()).isPresent()) {
            throw new BusinessException(AuthErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }

        socialAccountRepository.save(SocialAccount.of(member, provider, identity.providerUserId()));
    }

    public List<LinkedSocialAccountResponse> getLinkedAccounts(UUID memberId) {
        return socialAccountRepository.findAllByMember_Id(memberId).stream()
                .map(sa -> new LinkedSocialAccountResponse(sa.getProvider(), sa.getCreatedAt()))
                .toList();
    }

    @Transactional
    public AuthTokens reissue(String rawRefreshToken) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawRefreshToken);

        Member member = memberRepository.findById(result.memberId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        String accessToken = jwtTokenProvider.generateAccessToken(member);
        LoginResponse body = new LoginResponse(accessToken, "Bearer", jwtTokenProvider.getValidityMs() / 1000);

        return new AuthTokens(body, result.newRefreshToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public long getRefreshTokenValidityMs() {
        return refreshTokenService.getValidityMs();
    }

    private AuthTokens issueTokens(Member member) {
        String accessToken = jwtTokenProvider.generateAccessToken(member);
        String refreshToken = refreshTokenService.issue(member.getId());
        LoginResponse body = new LoginResponse(accessToken, "Bearer", jwtTokenProvider.getValidityMs() / 1000);

        return new AuthTokens(body, refreshToken);
    }

    private String normalize(String email) {
        return email.strip().toLowerCase();
    }

    /**
     * refreshToken은 응답 body가 아니라 HttpOnly 쿠키로 내려가므로,
     * Controller가 쿠키를 만들 수 있게 body(LoginResponse)와 refreshToken을 분리해서 반환한다.
     */
    public record AuthTokens(LoginResponse body, String refreshToken) {
    }
}
