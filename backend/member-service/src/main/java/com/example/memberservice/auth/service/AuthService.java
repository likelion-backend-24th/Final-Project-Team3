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
    public AuthTokens login(LoginRequest request) {
        Member member = memberRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(member);
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
