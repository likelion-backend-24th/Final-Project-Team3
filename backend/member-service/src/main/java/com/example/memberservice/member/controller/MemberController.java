package com.example.memberservice.member.controller;

import com.example.memberservice.auth.security.CustomUserDetails;
import com.example.memberservice.common.TraceIdProvider;
import com.example.memberservice.common.dto.ApiResponse;
import com.example.memberservice.member.dto.*;
import com.example.memberservice.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final TraceIdProvider traceIdProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest
    ) {
        SignupResponse response = memberService.signup(request);
        String traceId = traceIdProvider.resolve(httpRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response, traceId));
    }

    @PostMapping("/organizers/signup")
    public ResponseEntity<ApiResponse<OrganizerSignupResponse>> signupOrganizer(
            @Valid @RequestBody OrganizerSignupRequest request,
            HttpServletRequest httpRequest
    ) {
        OrganizerSignupResponse response = memberService.signupOrganizer(request);
        String traceId = traceIdProvider.resolve(httpRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("주최자 회원가입이 완료되었습니다.", response, traceId));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        MemberProfileResponse response = memberService.getProfile(currentUser.getMemberId());
        String traceId = traceIdProvider.resolve(httpRequest);
        return ResponseEntity.ok(ApiResponse.success("프로필 조회 성공", response, traceId));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser, // ← Step 3 필터가 채워준 값이 여기로 옴
            HttpServletRequest httpRequest
    ) {
        MemberProfileResponse response = memberService.updateProfile(currentUser.getMemberId(), request);
        String traceId = traceIdProvider.resolve(httpRequest);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", response, traceId));
    }
}