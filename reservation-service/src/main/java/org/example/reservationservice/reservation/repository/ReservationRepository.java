package org.example.reservationservice.reservation.repository;

import jakarta.persistence.LockModeType;
import org.example.reservationservice.reservation.entity.Reservation;
import org.example.reservationservice.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    // 특정 세션의 확정(CONFIRMED) 인원 합계 조회
    @Query("SELECT COALESCE(SUM(r.headcount), 0) FROM Reservation r " +
            "WHERE r.sessionId = :sessionId AND r.status = 'CONFIRMED'")
    int sumConfirmedHeadcountBySessionId(@Param("sessionId") UUID sessionId);

    // 동시성 제어용: 비관적 락으로 정원 계산 (동시 요청 대비)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COALESCE(SUM(r.headcount), 0) FROM Reservation r " +
            "WHERE r.sessionId = :sessionId AND r.status IN ('HOLD', 'CONFIRMED')")
    int sumConfirmedHeadcountBySessionIdForUpdate(@Param("sessionId") UUID sessionId);

    long countBySessionIdAndStatus(UUID sessionId, ReservationStatus status);
}
