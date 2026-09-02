package com.example.memberservice.member.service;

import com.example.memberservice.common.exception.BusinessException;
import com.example.memberservice.member.dto.SignupRequest;
import com.example.memberservice.member.dto.SignupResponse;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.exception.MemberErrorCode;
import com.example.memberservice.member.repository.MemberRepository;
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

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalize(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL, "이미 가입된 이메일입니다: " + email);
        }

        Member member = Member.newMember(email, passwordEncoder.encode(request.password()), request.name());
        try {
            memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL, "이미 가입된 이메일입니다: " + email);
        }

        return new SignupResponse(member.getId(), member.getEmail(), member.getName(), member.getRole().name());
    }

    private String normalize(String email) {
        return email.strip().toLowerCase();
    }
}
