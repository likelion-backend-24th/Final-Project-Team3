package com.example.reservationservice.settlement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.settlement.dto.SettlementResponse;
import com.example.reservationservice.settlement.service.SettlementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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
}
