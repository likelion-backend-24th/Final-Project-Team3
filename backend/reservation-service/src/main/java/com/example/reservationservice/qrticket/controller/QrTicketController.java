package com.example.reservationservice.qrticket.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.qrticket.dto.QrTicketResponse;
import com.example.reservationservice.qrticket.dto.QrTicketScanResponse;
import com.example.reservationservice.qrticket.service.QrTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "QR 티켓", description = "QR 티켓 조회·입장 스캔 API")
@RestController
@RequestMapping("/api/qr-tickets")
@RequiredArgsConstructor
public class QrTicketController {

    private final QrTicketService qrTicketService;
    private final TraceIdProvider traceIdProvider;

    @Operation(summary = "QR 티켓 조회", description = "본인의 결제 완료된 예약에 대한 QR 티켓 목록을 조회한다")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<List<QrTicketResponse>>> getQrTickets(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest) {
        List<QrTicketResponse> tickets = qrTicketService.getConfirmedTickets(reservationId, currentUser.getMemberId());
        return ResponseEntity.ok(
                ApiResponse.success("QR 티켓 조회 완료", tickets, traceIdProvider.resolve(httpRequest)));
    }

    @Operation(summary = "QR 입장 스캔", description = "세션 시작 이후, 해당 세션을 관리할 권한이 있는 주최자가 QR 코드를 스캔하여 입장 처리한다. 세션 시작 전이거나 이미 사용된 코드는 거부된다")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{code}/scan")
    public ResponseEntity<ApiResponse<QrTicketScanResponse>> scan(
            @PathVariable String code,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest) {
        QrTicketScanResponse response = qrTicketService.scan(code, currentUser.getMemberId());
        return ResponseEntity.ok(
                ApiResponse.success("QR 입장 처리 완료", response, traceIdProvider.resolve(httpRequest)));
    }
}