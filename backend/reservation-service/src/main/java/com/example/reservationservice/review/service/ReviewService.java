package com.example.reservationservice.review.service;

import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.repository.QrTicketRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.review.entity.Review;
import com.example.reservationservice.review.exception.ReviewErrorCode;
import com.example.reservationservice.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final QrTicketRepository qrTicketRepository;

    @Transactional
    public Review writeReview(UUID reservationId, UUID memberId, String content) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(()->new BusinessException(ReviewErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getMemberId().equals(memberId)) {
            throw new BusinessException(ReviewErrorCode.NOT_OWNER);
        }

        boolean eligible = qrTicketRepository.existsByReservationIdAndUsedTrue(reservationId);
        if (!eligible) {
            throw new BusinessException(ReviewErrorCode.NOT_ELIGIBLE);
        }

        return reviewRepository.findByReservationId(reservationId)
                .map(existing -> {
                    existing.updateContent(content);
                    return existing;
                })
                .orElseGet(() -> reviewRepository.save(
                        Review.builder()
                                .reservationId(reservationId)
                                .content(content)
                                .build()
                ));
    }
}
