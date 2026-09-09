package com.example.conferenceservice.notice.controller;

import com.example.conferenceservice.auth.CustomUserDetails;
import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.notice.dto.NoticeRequest;
import com.example.conferenceservice.notice.dto.NoticeResponse;
import com.example.conferenceservice.notice.service.NoticeService;
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
@RequestMapping("/api/conferences/{conferenceId}/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService noticeService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> listNotices(
            @PathVariable UUID conferenceId, HttpServletRequest request) {
        List<NoticeResponse> notices = noticeService.getNotices(conferenceId);
        return ResponseEntity.ok(ApiResponse.success("공지 목록 조회 성공", notices, traceIdProvider.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @PathVariable UUID conferenceId,
            @Valid @RequestBody NoticeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest) {
        NoticeResponse response = noticeService.createNotice(conferenceId, request, currentUser.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("공지 등록 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    @PatchMapping("/{noticeId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @PathVariable UUID conferenceId,
            @PathVariable UUID noticeId,
            @Valid @RequestBody NoticeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest) {
        NoticeResponse response = noticeService.updateNotice(noticeId, request, currentUser.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("공지 수정 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> deleteNotice(
            @PathVariable UUID conferenceId,
            @PathVariable UUID noticeId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        noticeService.deleteNotice(noticeId, currentUser.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
