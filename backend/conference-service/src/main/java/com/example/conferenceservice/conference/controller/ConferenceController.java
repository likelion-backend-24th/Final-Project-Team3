package com.example.conferenceservice.conference.controller;

import com.example.conferenceservice.auth.CustomUserDetails;
import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.common.dto.Meta;
import com.example.conferenceservice.common.dto.PageMeta;
import com.example.conferenceservice.conference.dto.ConferenceDescriptionUpdateRequest;
import com.example.conferenceservice.conference.dto.ConferenceDetailResponse;
import com.example.conferenceservice.conference.dto.ConferenceLocationUpdateRequest;
import com.example.conferenceservice.conference.dto.ConferenceRequest;
import com.example.conferenceservice.conference.dto.ConferenceResponse;
import com.example.conferenceservice.conference.service.ConferenceService;
import com.example.conferenceservice.session.dto.SessionCreateRequest;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conferences")
@RequiredArgsConstructor
public class ConferenceController {
    private final ConferenceService conferenceService;
    private final SessionService sessionService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConferenceResponse>>> listConferences(@PageableDefault Pageable pageable, HttpServletRequest request) {
        Page<ConferenceResponse> page = conferenceService.getConferences(pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("컨퍼런스 목록 조회 성공", page.getContent(), meta, traceIdProvider.resolve(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConferenceDetailResponse>> getConference(@PathVariable UUID id, HttpServletRequest request) {
        ConferenceDetailResponse conference = conferenceService.getConference(id);
        return ResponseEntity.ok(ApiResponse.success("컨퍼런스 상세 조회 성공", conference, traceIdProvider.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<ConferenceResponse>> createConference(
            @Valid @RequestBody ConferenceRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        ConferenceResponse response = conferenceService.applyConference(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("컨퍼런스 등록 신청 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    @PatchMapping("/{id}/description")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<ConferenceResponse>> updateDescription(
            @PathVariable UUID id,
            @Valid @RequestBody ConferenceDescriptionUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        ConferenceResponse response = conferenceService.updateDescription(id, request, currentUser.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("컨퍼런스 소개글 수정 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    @PatchMapping("/{id}/location")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<ConferenceResponse>> updateLocation(
            @PathVariable UUID id,
            @Valid @RequestBody ConferenceLocationUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        ConferenceResponse response = conferenceService.updateLocation(id, request, currentUser.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("컨퍼런스 장소 수정 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    @PostMapping("/{conferenceId}/sessions")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @PathVariable UUID conferenceId,
            @Valid @RequestBody SessionCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        SessionResponse response = sessionService.createSession(conferenceId, request, currentUser.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("세션 등록 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    @GetMapping("/{conferenceId}/sessions")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessionsByConference(
            @PathVariable UUID conferenceId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request
    ) {
        List<SessionResponse> sessions = sessionService.getSessionsByConference(conferenceId, currentUser.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("세션 목록 조회 성공", sessions, traceIdProvider.resolve(request)));
    }
}
