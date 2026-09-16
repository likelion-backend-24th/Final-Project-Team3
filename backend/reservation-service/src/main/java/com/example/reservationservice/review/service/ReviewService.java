package com.example.reservationservice.review.service;

import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.review.entity.Review;
import com.example.reservationservice.review.exception.ReviewErrorCode;
import com.example.reservationservice.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public List<String> getReviewsBySessionIds(List<UUID> sessionIds) {
        // 이 세션들에 속한 예약 전체의 ID
        List<UUID> reservationIds = reservationRepository.findBySessionIdIn(sessionIds).stream()
                .map(Reservation::getId)
                .toList();

        // 그 중 체크인 완료(used=true) 티켓이 하나라도 있는 예약만 추려냄 -> 한 예약에 동반자 티켓이 여러 장일 수 있어 distinct 필수
        List<UUID> checkedInReservationIds = qrTicketRepository.findByReservationIdInAndUsedTrue(reservationIds).stream()
                .map(QrTicket::getReservationId)
                .distinct()
                .toList();

        // 체크인 완료된 예약들의 후기 텍스트만 반환
        return reviewRepository.findByReservationIdIn(checkedInReservationIds).stream()
                .map(Review::getContent)
                .toList();
    }
}
