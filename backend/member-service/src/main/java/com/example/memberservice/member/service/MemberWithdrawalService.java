package com.example.memberservice.member.service;

import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.session.repository.SocialAccountRepository;
import com.example.memberservice.auth.session.service.RefreshTokenRevoker;
import com.example.memberservice.auth.social.SocialProvider;
import com.example.memberservice.auth.social.SocialTokenVerifier;
import com.example.memberservice.common.exception.BusinessException;
import com.example.memberservice.member.client.ConferenceServiceClient;
import com.example.memberservice.member.client.ConferenceServiceUnavailableException;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.exception.MemberErrorCode;
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
public class MemberWithdrawalService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SocialAccountRepository socialAccountRepository;
    private final List<SocialTokenVerifier> socialTokenVerifiers;
    private final ConferenceServiceClient conferenceServiceClient;
    private final RefreshTokenRevoker refreshTokenRevoker;

    @Transactional
    public void withdraw(UUID memberId, WithdrawRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        verifyIdentity(member, request);

        if (member.isOrganizer()) {
            verifyOrganizerCanWithdraw(member.getId());
        }

        member.withdraw();
        refreshTokenRevoker.revokeAll(member.getId());
    }

    private void verifyIdentity(Member member, WithdrawRequest request) {
        if (member.getPassword() != null) {
            if (request.password() == null || !passwordEncoder.matches(request.password(), member.getPassword())) {
                throw new BusinessException(MemberErrorCode.WITHDRAWAL_PASSWORD_MISMATCH);
            }
            return;
        }

        // 소셜 전용 계정 — 비밀번호가 없으니 소셜 재인증으로 본인 확인
        if (request.provider() == null || request.socialToken() == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_PROVIDER_UNSUPPORTED);
        }

        SocialProvider provider = parseProvider(request.provider());
        SocialTokenVerifier verifier = socialTokenVerifiers.stream()
                .filter(v -> v.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new BusinessException(AuthErrorCode.SOCIAL_PROVIDER_UNSUPPORTED));

        SocialTokenVerifier.VerifiedIdentity identity = verifier.verify(request.socialToken(), request.redirectUri());

        boolean linkedToThisMember = socialAccountRepository
                .findByProviderAndProviderUserId(provider, identity.providerUserId())
                .map(sa -> sa.getMember().getId().equals(member.getId()))
                .orElse(false);

        if (!linkedToThisMember) {
            throw new BusinessException(MemberErrorCode.WITHDRAWAL_SOCIAL_IDENTITY_MISMATCH);
        }
    }

    private void verifyOrganizerCanWithdraw(UUID organizerId) {
        boolean hasActiveConference;
        try {
            hasActiveConference = conferenceServiceClient.hasActiveConference(organizerId);
        } catch (ConferenceServiceUnavailableException e) {
            throw new BusinessException(MemberErrorCode.WITHDRAWAL_ELIGIBILITY_CHECK_FAILED);
        }

        if (hasActiveConference) {
            throw new BusinessException(MemberErrorCode.ORGANIZER_WITHDRAWAL_BLOCKED);
        }
    }

    private SocialProvider parseProvider(String provider) {
        try {
            return SocialProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.SOCIAL_PROVIDER_UNSUPPORTED);
        }
    }
}
