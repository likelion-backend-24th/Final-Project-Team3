package com.example.conferenceservice.conference.controller;

import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.common.dto.Meta;
import com.example.conferenceservice.common.dto.PageMeta;
import com.example.conferenceservice.conference.dto.ConferenceResponse;
import com.example.conferenceservice.conference.dto.RejectConferenceRequest;
import com.example.conferenceservice.conference.service.ConferenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/conferences")
@RequiredArgsConstructor
public class AdminConferenceController {

    private final ConferenceService conferenceService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ConferenceResponse>>> listPendingConferences(
            @PageableDefault Pageable pageable, HttpServletRequest request) {
        Page<ConferenceResponse> page = conferenceService.getPendingConferences(pageable).map(ConferenceResponse::from);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("승인 대기 컨퍼런스 목록 조회 성공", page.getContent(), meta, traceIdProvider.resolve(request)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConferenceResponse>> approveConference(
            @PathVariable UUID id, HttpServletRequest request) {
        ConferenceResponse response = conferenceService.approveConference(id);
        return ResponseEntity.ok(ApiResponse.success("컨퍼런스 승인 성공", response, traceIdProvider.resolve(request)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConferenceResponse>> rejectConference(
            @PathVariable UUID id,
            @Valid @RequestBody RejectConferenceRequest request,
            HttpServletRequest httpRequest) {
        ConferenceResponse response = conferenceService.rejectConference(id, request);
        return ResponseEntity.ok(ApiResponse.success("컨퍼런스 반려 성공", response, traceIdProvider.resolve(httpRequest)));
    }
}
