package com.example.conferenceservice.session.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.common.dto.Meta;
import com.example.conferenceservice.common.dto.PageMeta;
import com.example.conferenceservice.session.dto.RejectSessionRequest;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.service.SessionService;
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

@Tag(name = "세션 승인 (전체관리자)", description = "전체관리자(ADMIN) 전용 세션 승인·반려 API. ADMIN이 아니면 403")
@RestController
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {

    private final SessionService sessionService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "승인 대기 세션 목록", description = "PENDING 상태 세션을 페이지 단위로 조회한다. 페이지 정보는 meta.pagination에 담긴다")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> listPendingSessions(
            @PageableDefault Pageable pageable, HttpServletRequest request) {
        Page<SessionResponse> page = sessionService.getPendingSessions(pageable).map(SessionResponse::from);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("승인 대기 세션 목록 조회 성공", page.getContent(), meta, traceIdProvider.resolve(request)));
    }

    @Operation(summary = "세션 승인", description = "PENDING 세션을 APPROVED로 바꿔 즉시 공개한다. 이미 승인·반려된 세션은 409(SESSION_ALREADY_DECIDED), 없으면 404")
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SessionResponse>> approveSession(
            @PathVariable UUID id, HttpServletRequest request) {
        SessionResponse response = sessionService.approveSession(id);
        return ResponseEntity.ok(ApiResponse.success("세션 승인 성공", response, traceIdProvider.resolve(request)));
    }

    @Operation(summary = "세션 반려", description = "PENDING 세션을 REJECTED로 바꾸고 반려 사유를 저장한다. 사유가 비어 있으면 400, 이미 처리된 세션은 409(SESSION_ALREADY_DECIDED)")
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SessionResponse>> rejectSession(
            @PathVariable UUID id,
            @Valid @RequestBody RejectSessionRequest request,
            HttpServletRequest httpRequest) {
        SessionResponse response = sessionService.rejectSession(id, request);
        return ResponseEntity.ok(ApiResponse.success("세션 반려 성공", response, traceIdProvider.resolve(httpRequest)));
    }
}
