package com.example.reservationservice.qrticket.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.service.QrTicketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}