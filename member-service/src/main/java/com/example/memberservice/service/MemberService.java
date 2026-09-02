package com.example.memberservice.service;

import com.example.memberservice.dto.LoginRequest;
import com.example.memberservice.dto.LoginResponse;
import com.example.memberservice.dto.SignupRequest;
import com.example.memberservice.dto.SignupResponse;
import com.example.memberservice.entity.Member;
import com.example.memberservice.exception.DuplicateEmailException;
import com.example.memberservice.exception.InvalidCredentialsException;
import com.example.memberservice.repository.MemberRepository;
import com.example.memberservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalize(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        Member member = Member.newMember(email, passwordEncoder.encode(request.password()), request.name());
        try {
            memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(email);
        }

        return new SignupResponse(member.getId(), member.getEmail(), member.getName(), member.getRole().name());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(normalize(request.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(member);
        String refreshToken = refreshTokenService.issue(member.getId());

        return new LoginResponse(accessToken, refreshToken, "Bearer", jwtTokenProvider.getValidityMs() / 1000);
    }

    private String normalize(String email) {
        return email.strip().toLowerCase();
    }
}