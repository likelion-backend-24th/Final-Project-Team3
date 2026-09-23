package com.example.memberservice.member.controller;

import com.example.memberservice.auth.dto.LinkedSocialAccountResponse;
import com.example.memberservice.auth.security.CustomUserDetails;
import com.example.memberservice.auth.service.AuthService;
import com.example.memberservice.common.TraceIdProvider;
import com.example.memberservice.common.dto.ApiResponse;
import com.example.memberservice.member.dto.*;
import com.example.memberservice.member.service.MemberService;
import com.example.memberservice.member.service.OrganizerSignupFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final OrganizerSignupFacade organizerSignupFacade;
    private final AuthService authService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "참가자 회원가입", description = "이메일 인증을 완료한 이메일로 참가자(MEMBER) 계정을 생성한다.")
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

    @Operation(summary = "주최자 회원가입", description = "사업자등록번호가 국세청에 등록된 계속사업자로 조회되면(상태조회) 즉시 주최자(ORGANIZER) 계정을 생성한다. 미등록·휴업·폐업 번호는 400, 국세청 조회 장애는 503으로 거절한다.")
    @PostMapping("/organizers/signup")
    public ResponseEntity<ApiResponse<OrganizerSignupResponse>> signupOrganizer(
            @Valid @RequestBody OrganizerSignupRequest request,
            HttpServletRequest httpRequest
    ) {
        OrganizerSignupResponse response = organizerSignupFacade.signup(request);
        String traceId = traceIdProvider.resolve(httpRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("주최자 회원가입이 완료되었습니다.", response, traceId));
    }

    @Operation(summary = "내 프로필 조회", description = "Access Token으로 인증된 본인의 프로필을 조회한다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        MemberProfileResponse response = memberService.getProfile(currentUser.getMemberId());
        String traceId = traceIdProvider.resolve(httpRequest);
        return ResponseEntity.ok(ApiResponse.success("프로필 조회 성공", response, traceId));
    }

    @Operation(summary = "내 프로필 수정", description = "본인의 연령대·직무를 수정한다.")
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(summary = "연동된 소셜 계정 목록 조회", description = "본인 계정에 연동된 소셜 Provider 목록을 조회한다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/social-accounts")
    public ResponseEntity<ApiResponse<List<LinkedSocialAccountResponse>>> getLinkedSocialAccounts(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        List<LinkedSocialAccountResponse> response = authService.getLinkedAccounts(currentUser.getMemberId());
        String traceId = traceIdProvider.resolve(httpRequest);
        return ResponseEntity.ok(ApiResponse.success("연동된 소셜 계정 목록 조회 성공", response, traceId));
    }
}