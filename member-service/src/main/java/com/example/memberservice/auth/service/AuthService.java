package com.example.memberservice.auth.service;

import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.auth.dto.LoginResponse;
import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.security.JwtTokenProvider;
import com.example.memberservice.common.exception.BusinessException;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(member);
    }

    @Transactional
    public LoginResponse reissue(String rawRefreshToken) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawRefreshToken);

        Member member = memberRepository.findById(result.memberId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        String accessToken = jwtTokenProvider.generateAccessToken(member);

        return new LoginResponse(
                accessToken,
                result.newRefreshToken(),
                "Bearer",
                jwtTokenProvider.getValidityMs() / 1000
        );
    }

    private LoginResponse issueTokens(Member member) {
        String accessToken = jwtTokenProvider.generateAccessToken(member);
        String refreshToken = refreshTokenService.issue(member.getId());
        return new LoginResponse(accessToken, refreshToken, "Bearer", jwtTokenProvider.getValidityMs() / 1000);
    }

    private String normalize(String email) {
        return email.strip().toLowerCase();
    }
}
