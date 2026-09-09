package com.example.conferenceservice.faq.controller;

import com.example.conferenceservice.auth.CustomUserDetails;
import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.faq.dto.FaqRequest;
import com.example.conferenceservice.faq.dto.FaqResponse;
import com.example.conferenceservice.faq.service.FaqService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conferences/{conferenceId}/faqs")
@RequiredArgsConstructor
public class FaqController {
    private final FaqService faqService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqResponse>>> listFaqs(
            @PathVariable UUID conferenceId, HttpServletRequest request) {
        List<FaqResponse> faqs = faqService.getFaqs(conferenceId);
        return ResponseEntity.ok(ApiResponse.success("FAQ 목록 조회 성공", faqs, traceIdProvider.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<FaqResponse>> createFaq(
            @PathVariable UUID conferenceId,
            @Valid @RequestBody FaqRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest) {
        FaqResponse response = faqService.createFaq(conferenceId, request, currentUser.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("FAQ 등록 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    @PatchMapping("/{faqId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<FaqResponse>> updateFaq(
            @PathVariable UUID conferenceId,
            @PathVariable UUID faqId,
            @Valid @RequestBody FaqRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest) {
        FaqResponse response = faqService.updateFaq(faqId, request, currentUser.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("FAQ 수정 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    @DeleteMapping("/{faqId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> deleteFaq(
            @PathVariable UUID conferenceId,
            @PathVariable UUID faqId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        faqService.deleteFaq(faqId, currentUser.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
