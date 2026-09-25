package com.example.reservationservice.settlement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.common.dto.Meta;
import com.example.reservationservice.common.dto.PageMeta;
import com.example.reservationservice.settlement.dto.SettlementDetailResponse;
import com.example.reservationservice.settlement.dto.SettlementResponse;
import com.example.reservationservice.settlement.service.SettlementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "통합 정산 (전체관리자)", description = "전체관리자(ADMIN) 전용 정산 집계 API. ADMIN이 아니면 403")
@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final SettlementService settlementService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "통합 정산 대시보드", description = "결제 완료(CONFIRMED)된 예약의 결제 금액 합계를 반환한다. 취소된 예약은 집계에서 제외된다. "
            + "startDate·endDate(yyyy-MM-dd)를 주면 결제 시각이 그 기간에 속한 건만 집계하고(종료일 포함), 생략하면 전체 기간이다")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SettlementResponse>> getSettlementDashboard(
            @Parameter(description = "집계 시작일(포함), yyyy-MM-dd. 생략 시 처음부터", example = "2026-09-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "집계 종료일(포함), yyyy-MM-dd. 생략 시 끝까지", example = "2026-09-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest httpRequest) {
        SettlementResponse response = settlementService.getSettlementDashboard(startDate, endDate);
        return ResponseEntity.ok(
                ApiResponse.success("정산 대시보드 조회 완료", response, traceIdProvider.resolve(httpRequest)));
    }

    @Operation(summary = "정산 내역 상세 조회", description = "결제 건별로 세션 ID, 결제 금액, 예약 상태(CONFIRMED/CANCELLED), 환불 정보, "
            + "체크인(QR 사용) 현황을 페이지 단위로 반환한다. 취소된 결제 건도 CANCELLED 상태로 포함된다. "
            + "세션·컨퍼런스 이름은 응답에 없어 프론트가 공개 컨퍼런스 목록으로 sessionId를 매핑해야 한다. "
            + "startDate·endDate(yyyy-MM-dd)를 주면 결제 시각이 그 기간에 속한 건만 조회하고(종료일 포함), 생략하면 전체 기간이다")
    @GetMapping("/details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SettlementDetailResponse>>> getSettlementDetails(
            @Parameter(description = "조회 시작일(포함), yyyy-MM-dd. 생략 시 처음부터", example = "2026-09-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일(포함), yyyy-MM-dd. 생략 시 끝까지", example = "2026-09-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest httpRequest) {
        Page<SettlementDetailResponse> page = settlementService.getSettlementDetails(startDate, endDate, pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(
                ApiResponse.success("정산 내역 상세 조회 완료", page.getContent(), meta, traceIdProvider.resolve(httpRequest)));
    }
}
