package com.example.reservationservice.review.service;

import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.review.entity.Review;
import com.example.reservationservice.review.repository.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReviewUniquenessTest {

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
    void 이미_후기가_있는_예약으로_재작성하면_새로_생기지_않고_내용만_수정된다() {
        UUID memberId = UUID.randomUUID();
        Reservation reservation = reservationRepository.save(
                Reservation.builder().sessionId(UUID.randomUUID()).memberId(memberId).headcount(1).build());

        QrTicket ticket = QrTicket.builder()
                .reservationId(reservation.getId())
                .code("CODE-4")
                .ageGroup(AgeGroup.TWENTIES)
                .job(Job.DEVELOPER)
                .build();
        ticket.markAsUsed();
        qrTicketRepository.save(ticket);

        reviewService.writeReview(reservation.getId(), memberId, "처음 작성한 후기");
        Review updated = reviewService.writeReview(reservation.getId(), memberId, "수정한 후기");

        assertThat(reviewRepository.count()).isEqualTo(1);
        assertThat(updated.getContent()).isEqualTo("수정한 후기");

        Review stored = reviewRepository.findByReservationId(reservation.getId()).orElseThrow();
        assertThat(stored.getContent()).isEqualTo("수정한 후기");
    }
}
