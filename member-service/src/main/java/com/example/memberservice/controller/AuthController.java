package com.example.memberservice.controller;

import com.example.memberservice.dto.LoginRequest;
import com.example.memberservice.dto.LoginResponse;
import com.example.memberservice.dto.RefreshRequest;
import com.example.memberservice.security.JwtTokenProvider;
import com.example.memberservice.repository.MemberRepository;
import com.example.memberservice.entity.Member;
import com.example.memberservice.exception.InvalidCredentialsException;
import com.example.memberservice.service.MemberService;
import com.example.memberservice.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(request.refreshToken());

        Member member = memberRepository.findById(result.memberId())
                .orElseThrow(InvalidCredentialsException::new);

        String accessToken = jwtTokenProvider.generateAccessToken(member);

        return ResponseEntity.ok(new LoginResponse(
                accessToken,
                result.newRefreshToken(),
                "Bearer",
                jwtTokenProvider.getValidityMs() / 1000
        ));
    }
}