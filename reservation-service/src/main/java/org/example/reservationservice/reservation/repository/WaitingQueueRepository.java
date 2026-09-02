package org.example.reservationservice.reservation.repository;

import org.example.reservationservice.reservation.entity.WaitingQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WaitingQueueRepository extends JpaRepository<WaitingQueue, UUID> {

    // 특정 세션의 현재 마지막 순번 조회
    @Query("SELECT COALESCE(MAX(w.position), 0) FROM WaitingQueue w WHERE w.sessionId = :sessionId")
    int findMaxPositionBySessionId(@Param("sessionId") UUID sessionId);

    // 예약 ID로 대기열 항목 조회 (순번 조회 API용)
    Optional<WaitingQueue> findByReservationId(UUID reservationId);

    @Query(value = "SELECT COALESCE(MAX(position), 0) + 1 FROM waiting_queue " +
            "WHERE session_id = :sessionId FOR UPDATE",
            nativeQuery = true)
    int getNextPositionForUpdate(@Param("sessionId") UUID sessionId);
}
