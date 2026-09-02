package org.example.reservationservice.reservation.repository;

import org.example.reservationservice.reservation.entity.SessionCapacityLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface SessionCapacityLockRepository extends JpaRepository<SessionCapacityLock, UUID> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO session_capacity_lock (session_id, current_active) " +
            "VALUES (:sessionId, 0) " +
            "ON DUPLICATE KEY UPDATE session_id = session_id",
            nativeQuery = true)
    void ensureExists(@Param("sessionId") UUID sessionId);

    @Modifying
    @Transactional
    @Query("UPDATE SessionCapacityLock s SET s.currentActive = s.currentActive + :headcount " +
            "WHERE s.sessionId = :sessionId AND s.currentActive + :headcount <= :capacity")
    int tryIncrease(@Param("sessionId") UUID sessionId,
                    @Param("headcount") int headcount,
                    @Param("capacity") int capacity);
}