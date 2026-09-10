package com.example.conferenceservice.session.controller;

import com.example.conferenceservice.auth.CustomUserDetails;
import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.session.dto.SessionCapacityResponse;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.dto.SessionStartAtResponse;
import com.example.conferenceservice.session.dto.SessionUpdateRequest;
import com.example.conferenceservice.session.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping("/{sessionId}/capacity")
    public ResponseEntity<ApiResponse<SessionCapacityResponse>> getCapacity(@PathVariable UUID sessionId, HttpServletRequest request) {
        SessionCapacityResponse capacity = sessionService.getCapacity(sessionId);
        return ResponseEntity.ok(ApiResponse.success("세션 정원 조회 성공", capacity, traceIdProvider.resolve(request)));
    }

    @PatchMapping("/{sessionId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<SessionResponse>> updateSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SessionUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        SessionResponse response = sessionService.updateSession(sessionId, request, currentUser.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("세션 정원·일정 수정 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    @GetMapping("/{sessionId}/startat")
    public ResponseEntity<ApiResponse<SessionStartAtResponse>> getStartAt(@PathVariable UUID sessionId, HttpServletRequest request) {
        SessionStartAtResponse startAt = sessionService.getStartAt(sessionId);
        return ResponseEntity.ok(ApiResponse.success("세션 시작 일시 조회 성공", startAt, traceIdProvider.resolve(request)));
    }
}
