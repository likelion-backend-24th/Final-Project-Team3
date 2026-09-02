package org.example.reservationservice.reservation.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.reservationservice.common.TraceIdProvider;
import org.example.reservationservice.common.dto.ApiResponse;
import org.example.reservationservice.reservation.dto.ReservationResult;
import org.example.reservationservice.reservation.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final TraceIdProvider traceIdProvider;

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<ReservationResult>> createHold(
            @RequestBody CreateHoldRequest request, HttpServletRequest httpRequest) {
        ReservationResult result = reservationService.createHoldOrQueue(
                request.sessionId(),
                request.memberId(),
                request.headcount()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("예약 처리 완료", result, traceIdProvider.resolve(httpRequest)));
    }

    @GetMapping("/{reservationId}/queue-position")
    public ResponseEntity<ApiResponse<Integer>> getQueuePosition(
            @PathVariable UUID reservationId, HttpServletRequest httpRequest) {
        int position = reservationService.getQueuePosition(reservationId);
        return ResponseEntity.ok(
                ApiResponse.success("순번 조회 완료", position, traceIdProvider.resolve(httpRequest)));
    }

    public record CreateHoldRequest(UUID sessionId, UUID memberId, int headcount) {}
}