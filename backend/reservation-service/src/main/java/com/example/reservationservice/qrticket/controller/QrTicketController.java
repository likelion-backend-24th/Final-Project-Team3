package com.example.reservationservice.qrticket.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.qrticket.dto.QrTicketScanResponse;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.service.QrTicketService;
import com.example.reservationservice.reservation.entity.Reservation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @Operation(summary = "QR 티켓 조회", description = "결제 완료된 예약의 QR 티켓 목록을 조회한다")
    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<List<QrTicket>>> getQrTickets(
            @PathVariable UUID reservationId,
            HttpServletRequest httpRequest) {
        List<QrTicket> tickets = qrTicketService.getConfirmedTickets(reservationId);
        return ResponseEntity.ok(
                ApiResponse.success("QR 티켓 조회 완료", tickets, traceIdProvider.resolve(httpRequest)));
    }

    @Operation(summary = "QR 입장 스캔", description = "세션 시작 이후, 주최자(ORGANIZER)가 QR 코드를 스캔하여 입장 처리한다. 세션 시작 전이거나 이미 사용된 코드는 거부된다")
    @PostMapping("/{code}/scan")
    public ResponseEntity<ApiResponse<QrTicketScanResponse>> scan(
            @PathVariable String code,
            HttpServletRequest httpRequest) {
        QrTicketScanResponse response = qrTicketService.scan(code);
        return ResponseEntity.ok(
                ApiResponse.success("QR 입장 처리 완료", response, traceIdProvider.resolve(httpRequest)));
    }
}