package com.example.reservationservice.reservation.repository;

import com.example.reservationservice.reservation.entity.QrTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QrTicketRepository extends JpaRepository<QrTicket, UUID> {
    List<QrTicket> findByReservationId(UUID reservationId);
    boolean existsByReservationIdAndUsedTrue(UUID reservationId);
    List<QrTicket> findByReservationIdInAndUsedTrue(List<UUID> reservationIds);

    @Query("SELECT COUNT(q) FROM QrTicket q " +
            "JOIN Reservation r ON q.reservationId = r.id " +
            "WHERE r.sessionId = :sessionId AND q.used = true")
    long countCheckedInBySessionId(@Param("sessionId") UUID sessionId);
}