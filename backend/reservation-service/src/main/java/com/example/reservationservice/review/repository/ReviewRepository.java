package com.example.reservationservice.review.repository;

import com.example.reservationservice.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByReservationId(UUID reservationId);

    // 체크인 완료된 예약 ID 목록으로 후기를 한 번에 조회 -> 15-5 Conference-Service 요약용
    List<Review> findByReservationIdIn(List<UUID> reservationIds);
}
