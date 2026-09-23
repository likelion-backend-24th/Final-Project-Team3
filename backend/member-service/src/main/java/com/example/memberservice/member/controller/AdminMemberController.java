package com.example.memberservice.member.controller;

import com.example.memberservice.common.TraceIdProvider;
import com.example.memberservice.common.dto.ApiResponse;
import com.example.memberservice.common.dto.Meta;
import com.example.memberservice.common.dto.PageMeta;
import com.example.memberservice.member.dto.ChangeRoleRequest;
import com.example.memberservice.member.dto.MemberDetailResponse;
import com.example.memberservice.member.dto.MemberListResponse;
import com.example.memberservice.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "전체 유저 목록 조회", description = "이메일·이름으로 검색 가능한 유저 목록을 페이징하여 조회한다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberListResponse>>> getMembers(
            @RequestParam(required = false) String keyword,
            @PageableDefault Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        Page<MemberListResponse> page = memberService.getMembers(keyword, pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(
                ApiResponse.success("유저 목록 조회 성공", page.getContent(), meta, traceIdProvider.resolve(httpRequest)));
    }

    @Operation(summary = "유저 상세 조회", description = "특정 유저의 상세 정보를 조회한다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<MemberDetailResponse>> getMemberDetail(
            @PathVariable UUID memberId,
            HttpServletRequest httpRequest
    ) {
        MemberDetailResponse response = memberService.getMemberDetail(memberId);
        return ResponseEntity.ok(
                ApiResponse.success("유저 상세 조회 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    @Operation(summary = "유저 권한 변경", description = "특정 유저의 역할(MEMBER/ORGANIZER/ADMIN)을 변경한다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{memberId}/role")
    public ResponseEntity<ApiResponse<MemberDetailResponse>> changeRole(
            @PathVariable UUID memberId,
            @Valid @RequestBody ChangeRoleRequest request,
            HttpServletRequest httpRequest
    ) {
        MemberDetailResponse response = memberService.changeRole(memberId, request);
        return ResponseEntity.ok(
                ApiResponse.success("권한이 변경되었습니다.", response, traceIdProvider.resolve(httpRequest)));
    }
}