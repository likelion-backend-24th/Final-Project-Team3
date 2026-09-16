package com.example.reservationservice.qrticket.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.qrticket.dto.QrTicketScanResponse;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.service.QrTicketService;
import com.example.reservationservice.reservation.entity.Reservation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/qr-tickets")
@RequiredArgsConstructor
public class QrTicketController {

    private final QrTicketService qrTicketService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<List<QrTicket>>> getQrTickets(
            @PathVariable UUID reservationId,
            HttpServletRequest httpRequest) {
        List<QrTicket> tickets = qrTicketService.getConfirmedTickets(reservationId);
        return ResponseEntity.ok(
                ApiResponse.success("QR 티켓 조회 완료", tickets, traceIdProvider.resolve(httpRequest)));
    }

    @PostMapping("/{code}/scan")
    public ResponseEntity<ApiResponse<QrTicketScanResponse>> scan(
            @PathVariable String code,
            HttpServletRequest httpRequest) {
        QrTicketScanResponse response = qrTicketService.scan(code);
        return ResponseEntity.ok(
                ApiResponse.success("QR 입장 처리 완료", response, traceIdProvider.resolve(httpRequest)));
    }
}