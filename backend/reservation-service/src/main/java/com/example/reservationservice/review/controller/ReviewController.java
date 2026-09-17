package com.example.reservationservice.review.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.common.TraceIdProvider;
import com.example.reservationservice.common.dto.ApiResponse;
import com.example.reservationservice.review.dto.CreateReviewRequest;
import com.example.reservationservice.review.dto.ReviewResponse;
import com.example.reservationservice.review.entity.Review;
import com.example.reservationservice.review.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations/{reservationId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final TraceIdProvider traceIdProvider;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest) {

        Review review = reviewService.writeReview(reservationId, userDetails.getMemberId(), request.content());

        return ResponseEntity.ok(
                ApiResponse.success("후기가 저장되었습니다", ReviewResponse.from(review),
                        traceIdProvider.resolve(httpRequest)));
    }
}
