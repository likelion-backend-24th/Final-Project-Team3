package com.example.reservationservice.review.controller;

import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.review.dto.ReviewListResponse;
import com.example.reservationservice.review.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/reservations")
@RequiredArgsConstructor
public class InternalReviewController {

    private final ReviewService reviewService;
    private final TraceIdProvider traceIdProvider;

    // 인증 없음(permitAll) — Task 15-1의 attendee-checkin-stats와 동일한 서비스 간 내부 호출용 엔드포인트
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewListResponse>> getReviews(
            @RequestParam List<UUID> sessionIds,
            HttpServletRequest httpRequest) {
        List<String> reviews = reviewService.getReviewsBySessionIds(sessionIds);
        return ResponseEntity.ok(
                ApiResponse.success("후기 목록 조회 완료", new ReviewListResponse(reviews),
                        traceIdProvider.resolve(httpRequest)));
    }
}