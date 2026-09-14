package com.example.reservationservice.reservation.repository;

import com.example.reservationservice.reservation.entity.Attendee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendeeRepository extends JpaRepository<Attendee, UUID> {
    List<Attendee> findByReservationId(UUID reservationId);
}