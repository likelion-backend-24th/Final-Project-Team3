package com.example.reservationservice.review.service;

import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;
import com.example.reservationservice.reservation.entity.QrTicket;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.repository.QrTicketRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.review.exception.ReviewErrorCode;
import com.example.reservationservice.review.repository.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReviewEligibilityTest {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private QrTicketRepository qrTicketRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
        qrTicketRepository.deleteAll();
        reservationRepository.deleteAll();
    }

    @Test
    void 체크인_완료_좌석이_있으면_후기를_작성할_수_있다() {
        UUID memberId = UUID.randomUUID();
        Reservation reservation = reservationRepository.save(
                Reservation.builder().sessionId(UUID.randomUUID()).memberId(memberId).headcount(1).build());

        QrTicket ticket = QrTicket.builder()
                .reservationId(reservation.getId())
                .code("CODE-1")
                .ageGroup(AgeGroup.TWENTIES)
                .job(Job.DEVELOPER)
                .build();
        ticket.markAsUsed();
        qrTicketRepository.save(ticket);

        var review = reviewService.writeReview(reservation.getId(), memberId, "좋은 세션이었어요");

        assertThat(review.getContent()).isEqualTo("좋은 세션이었어요");
    }

    @Test
    void 체크인_완료_좌석이_없으면_거부된다() {
        UUID memberId = UUID.randomUUID();
        Reservation reservation = reservationRepository.save(
                Reservation.builder().sessionId(UUID.randomUUID()).memberId(memberId).headcount(1).build());

        qrTicketRepository.save(QrTicket.builder()
                .reservationId(reservation.getId())
                .code("CODE-2")
                .ageGroup(AgeGroup.TWENTIES)
                .job(Job.DEVELOPER)
                .build()); // markAsUsed() 호출 안 함 = 체크인 안 된 상태

        assertThatThrownBy(() -> reviewService.writeReview(reservation.getId(), memberId, "후기"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ReviewErrorCode.NOT_ELIGIBLE);
    }

    @Test
    void 존재하지_않는_예약이면_거부된다() {
        assertThatThrownBy(() -> reviewService.writeReview(UUID.randomUUID(), UUID.randomUUID(), "후기"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ReviewErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    void 본인_예약이_아니면_거부된다() {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        Reservation reservation = reservationRepository.save(
                Reservation.builder().sessionId(UUID.randomUUID()).memberId(ownerId).headcount(1).build());

        QrTicket ticket = QrTicket.builder()
                .reservationId(reservation.getId())
                .code("CODE-3")
                .ageGroup(AgeGroup.TWENTIES)
                .job(Job.DEVELOPER)
                .build();
        ticket.markAsUsed();
        qrTicketRepository.save(ticket);

        assertThatThrownBy(() -> reviewService.writeReview(reservation.getId(), strangerId, "후기"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ReviewErrorCode.NOT_OWNER);
    }
}
