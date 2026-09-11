package com.example.reservationservice.reservation.repository;

import com.example.reservationservice.reservation.entity.WaitingQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // 세션의 대기열 맨 앞(가장 낮은 순번) 조회 (좌석 반납 시 승격 대상 판단용)
    Optional<WaitingQueue> findFirstBySessionIdOrderByPositionAsc(UUID sessionId);

    @Query(value = "SELECT COALESCE(MAX(position), 0) + 1 FROM waiting_queue " +
            "WHERE session_id = :sessionId FOR UPDATE",
            nativeQuery = true)
    int getNextPositionForUpdate(@Param("sessionId") UUID sessionId);

    // 대기열에서 이탈(결제 완료 등)한 예약의 항목 삭제
    @Modifying
    void deleteByReservationId(UUID reservationId0);

    @Modifying
    @Query("UPDATE WaitingQueue w SET w.position = w.position - 1 " +
           "WHERE w.sessionId = :sessionId AND w.position > :leftPosition")
    void decrementPositionAfter(@Param("sessionId") UUID sessionId, @Param("leftPosition") int leftPosition);
}
