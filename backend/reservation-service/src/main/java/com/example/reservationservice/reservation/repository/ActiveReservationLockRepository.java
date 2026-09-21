package com.example.reservationservice.reservation.repository;

import com.example.reservationservice.reservation.entity.ActiveReservationLock;
import com.example.reservationservice.reservation.entity.ActiveReservationLockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ActiveReservationLockRepository extends JpaRepository<ActiveReservationLock, ActiveReservationLockId> {

    @Modifying
    @Query("DELETE FROM ActiveReservationLock l WHERE l.reservationId = :reservationId")
    void deleteByReservationId(@Param("reservationId") UUID reservationId);
}