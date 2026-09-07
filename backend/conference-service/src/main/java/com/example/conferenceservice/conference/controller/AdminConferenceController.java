package com.example.conferenceservice.conference.controller;

import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.common.dto.Meta;
import com.example.conferenceservice.common.dto.PageMeta;
import com.example.conferenceservice.conference.dto.ConferenceResponse;
import com.example.conferenceservice.conference.service.ConferenceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
