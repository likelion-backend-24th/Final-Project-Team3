package com.example.conferenceservice.conference.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.common.dto.Meta;
import com.example.conferenceservice.common.dto.PageMeta;
import com.example.conferenceservice.conference.dto.ConferenceDetailResponse;
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

@Tag(name = "컨퍼런스 승인 (전체관리자)", description = "전체관리자(ADMIN) 전용 컨퍼런스 승인·반려 API. ADMIN이 아니면 403")
@RestController
@RequestMapping("/api/admin/conferences")
@RequiredArgsConstructor
public class AdminConferenceController {

    private final ConferenceService conferenceService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "승인 대기 컨퍼런스 목록", description = "PENDING 상태 컨퍼런스를 페이지 단위로 조회한다. 페이지 정보는 meta.pagination에 담긴다")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ConferenceResponse>>> listPendingConferences(
            @PageableDefault Pageable pageable, HttpServletRequest request) {
        Page<ConferenceResponse> page = conferenceService.getPendingConferenceResponses(pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("승인 대기 컨퍼런스 목록 조회 성공", page.getContent(), meta, traceIdProvider.resolve(request)));
    }

    @Operation(summary = "컨퍼런스 상세(관리자)", description = "승인 심사용 상세 조회. 승인 전(PENDING)·반려 컨퍼런스도 조회되고 세션 전체와 주최자 이력, 증명 파일 첨부 여부를 포함한다. 없으면 404")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConferenceDetailResponse>> getConferenceDetail(
            @PathVariable UUID id, HttpServletRequest request) {
        ConferenceDetailResponse response = conferenceService.getConferenceDetailForAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("컨퍼런스 상세 조회 성공", response, traceIdProvider.resolve(request)));
    }

    @Operation(summary = "컨퍼런스 승인", description = "PENDING 컨퍼런스를 APPROVED로 바꿔 즉시 공개한다. 이미 승인·반려된 컨퍼런스는 409(CONFERENCE_ALREADY_DECIDED), 없으면 404")
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConferenceResponse>> approveConference(
            @PathVariable UUID id, HttpServletRequest request) {
        ConferenceResponse response = conferenceService.approveConference(id);
        return ResponseEntity.ok(ApiResponse.success("컨퍼런스 승인 성공", response, traceIdProvider.resolve(request)));
    }

    @Operation(summary = "컨퍼런스 반려", description = "PENDING 컨퍼런스를 REJECTED로 바꾸고 반려 사유를 저장한다. 사유가 비어 있으면 400, 이미 처리된 컨퍼런스는 409(CONFERENCE_ALREADY_DECIDED)")
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
