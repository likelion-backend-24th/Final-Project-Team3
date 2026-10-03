package com.example.reservationservice.reservation.repository;

import jakarta.persistence.LockModeType;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    long countBySessionIdAndStatus(UUID sessionId, ReservationStatus status);
    boolean existsBySessionIdAndMemberIdAndStatusIn(UUID sessionId, UUID memberId, List<ReservationStatus> statuses);

    // 예약 건수가 아니라 인원 수(headcount 합)로 세야 QR 발급 수(1인당 1장)·정원 잠금(headcount만큼 증가)과 단위가 맞는다.
    @Query("SELECT COALESCE(SUM(r.headcount), 0) FROM Reservation r WHERE r.sessionId = :sessionId AND r.status = :status")
    long sumHeadcountBySessionIdAndStatus(@Param("sessionId") UUID sessionId, @Param("status") ReservationStatus status);

    @Modifying
    @Query("UPDATE Reservation r SET r.status = 'CONFIRMED' " +
            "WHERE r.id = :id AND r.status = 'HOLD'")
    int confirmIfNotAlready(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Reservation r SET r.status = 'HOLD', r.expiresAt = :expiresAt, r.holdStartedAt = :holdStartedAt " +
            "WHERE r.id = :id AND r.status = 'QUEUED'")
    int promoteToHoldIfQueued(@Param("id") UUID id,
                              @Param("expiresAt") LocalDateTime expiresAt,
                              @Param("holdStartedAt") LocalDateTime holdStartedAt);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime time);
    // 세션의 결제 대기(HOLD) 예약을 만료가 빠른 순서로 조회 (대기자 예상 대기 시간 계산용)
    List<Reservation> findBySessionIdAndStatusOrderByExpiresAtAsc(UUID sessionId, ReservationStatus status);
    List<Reservation> findByMemberIdOrderByCreatedAtDesc(UUID memberId);
    List<Reservation> findBySessionIdIn(List<UUID> sessionIds);

    @Query("SELECT r.status, COALESCE(SUM(r.headcount), 0) FROM Reservation r " +
           "WHERE r.sessionId = :sessionId GROUP BY r.status")
    List<Object[]> sumHeadcountGroupByStatus(@Param("sessionId") UUID sessionId);
}



