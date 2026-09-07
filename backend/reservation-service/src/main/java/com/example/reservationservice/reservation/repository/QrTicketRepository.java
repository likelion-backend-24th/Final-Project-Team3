package com.example.reservationservice.reservation.repository;

import com.example.reservationservice.reservation.entity.QrTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QrTicketRepository extends JpaRepository<QrTicket, UUID> {
    List<QrTicket> findByReservationId(UUID reservationId);
}
