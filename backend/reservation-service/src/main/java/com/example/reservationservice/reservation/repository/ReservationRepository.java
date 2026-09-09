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

    @Modifying
    @Query("UPDATE Reservation r SET r.status = 'CONFIRMED' " +
            "WHERE r.id = :id AND r.status != 'CONFIRMED'")
    int confirmIfNotAlready(@Param("id") UUID id);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime time);
    List<Reservation> findByMemberIdOrderByCreatedAtDesc(UUID memberId);
}



