package com.example.conferenceservice.conference.controller;

import com.example.conferenceservice.attendeesummary.dto.ConferenceAttendeeSummaryResponse;
import com.example.conferenceservice.attendeesummary.service.ConferenceAttendeeSummaryService;
import com.example.conferenceservice.auth.CustomUserDetails;
import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.common.dto.Meta;
import com.example.conferenceservice.common.dto.PageMeta;
import com.example.conferenceservice.common.file.FileStorageService;
import com.example.conferenceservice.conference.dto.ConferenceDescriptionUpdateRequest;
import com.example.conferenceservice.conference.dto.ConferenceDetailResponse;
import com.example.conferenceservice.conference.dto.ConferenceLocationUpdateRequest;
import com.example.conferenceservice.conference.dto.ConferenceRequest;
import com.example.conferenceservice.conference.dto.ConferenceResponse;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.service.ConferenceService;
import com.example.conferenceservice.operationstatus.dto.ConferenceOperationStatusResponse;
import com.example.conferenceservice.operationstatus.service.ConferenceOperationStatusService;
import com.example.conferenceservice.settlement.dto.ConferenceSettlementResponse;
import com.example.conferenceservice.settlement.service.ConferenceSettlementService;
import com.example.conferenceservice.session.dto.SessionCreateRequest;
import com.example.conferenceservice.session.dto.SessionResponse;
import com.example.conferenceservice.session.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/conferences")
@RequiredArgsConstructor
public class ConferenceController {
    private final ConferenceService conferenceService;
    private final SessionService sessionService;
    private final ConferenceOperationStatusService conferenceOperationStatusService;
    private final ConferenceAttendeeSummaryService conferenceAttendeeSummaryService;
    private final ConferenceSettlementService conferenceSettlementService;
    private final FileStorageService fileStorageService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConferenceResponse>>> listConferences(@PageableDefault Pageable pageable, HttpServletRequest request) {
        Page<ConferenceResponse> page = conferenceService.getConferences(pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("컨퍼런스 목록 조회 성공", page.getContent(), meta, traceIdProvider.resolve(request)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<List<ConferenceResponse>>> getMyConferences(
            @PageableDefault Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request
    ) {
        Page<ConferenceResponse> page = conferenceService.getMyConferences(currentUser.getMemberId(), pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("내 컨퍼런스 목록 조회 성공", page.getContent(), meta, traceIdProvider.resolve(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConferenceDetailResponse>> getConference(@PathVariable UUID id, HttpServletRequest request) {
        ConferenceDetailResponse conference = conferenceService.getConference(id);
        return ResponseEntity.ok(ApiResponse.success("컨퍼런스 상세 조회 성공", conference, traceIdProvider.resolve(request)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<ConferenceResponse>> createConference(
            @Valid @RequestPart("request") ConferenceRequest request,
            @RequestPart(value = "proofFile", required = false) MultipartFile proofFile,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        ConferenceResponse response = conferenceService.applyConference(currentUser, request, proofFile, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("컨퍼런스 등록 신청 성공", response, traceIdProvider.resolve(httpRequest)));
    }

    // 썸네일·상세 이미지는 목록/상세 조회와 마찬가지로 비공개 정보가 아니라 인증 없이 공개한다.
    // 파일명에 UUID가 섞여 있어 값이 바뀌면 URL도 바뀌므로 장기 캐시가 안전하다.
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource resource = fileStorageService.loadImageAsResource(filename);
        return ResponseEntity.ok()
                .contentType(resolveImageMediaType(filename))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(resource);
    }

    private MediaType resolveImageMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        return MediaType.IMAGE_JPEG;
    }

    // 증명 파일은 승인 심사용 자료라 공개하지 않고, 소유 주최자 본인과 관리자만 내려받을 수 있다.
    @GetMapping("/{id}/proof-file")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Resource> downloadProofFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Conference conference = conferenceService.getConferenceForProofFileAccess(id, currentUser);
        Resource resource = fileStorageService.loadAsResource(conference.getProofFileName());
        String originalFilename = fileStorageService.extractOriginalFilename(conference.getProofFileName());
        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
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

    @GetMapping("/{conferenceId}/operation-status")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<ConferenceOperationStatusResponse>> getOperationStatus(
            @PathVariable UUID conferenceId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request
    ) {
        ConferenceOperationStatusResponse response =
                conferenceOperationStatusService.getOperationStatus(conferenceId, currentUser.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("세션별 신청·입장 현황 조회 성공", response, traceIdProvider.resolve(request)));
    }

    @GetMapping("/{conferenceId}/attendee-summary")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<ConferenceAttendeeSummaryResponse>> getAttendeeSummary(
            @PathVariable UUID conferenceId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request
    ) {
        ConferenceAttendeeSummaryResponse response =
                conferenceAttendeeSummaryService.getAttendeeSummary(conferenceId, currentUser.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("참석자 통계 조회 성공", response, traceIdProvider.resolve(request)));
    }

    @GetMapping("/{conferenceId}/reviews")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<List<String>>> getReviews(
            @PathVariable UUID conferenceId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request
    ) {
        List<String> reviews = conferenceAttendeeSummaryService.getReviews(conferenceId, currentUser.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("후기 목록 조회 성공", reviews, traceIdProvider.resolve(request)));
    }

    @GetMapping("/{conferenceId}/settlement")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<ConferenceSettlementResponse>> getSettlement(
            @PathVariable UUID conferenceId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request
    ) {
        ConferenceSettlementResponse response =
                conferenceSettlementService.getSettlement(conferenceId, currentUser.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("정산 조회 성공", response, traceIdProvider.resolve(request)));
    }
}
