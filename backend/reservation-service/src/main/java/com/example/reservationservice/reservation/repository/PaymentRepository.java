package com.example.reservationservice.reservation.repository;

import com.example.reservationservice.reservation.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "JOIN Reservation r ON p.reservationId = r.id " +
            "WHERE r.sessionId IN :sessionIds AND r.status = 'CONFIRMED'")
    int sumConfirmedAmount(@Param("sessionIds") List<UUID> sessionIds);

    @Query("SELECT COUNT(p) FROM Payment p " +
            "JOIN Reservation r ON p.reservationId = r.id " +
            "WHERE r.sessionId IN :sessionIds AND r.status = 'CONFIRMED'")
    int countConfirmed(@Param("sessionIds") List<UUID> sessionIds);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "JOIN Reservation r ON p.reservationId = r.id " +
            "WHERE r.sessionId IN :sessionIds AND r.status = 'CANCELLED'")
    int sumRefundedAmount(@Param("sessionIds") List<UUID> sessionIds);

    @Query("SELECT COUNT(p) FROM Payment p " +
            "JOIN Reservation r ON p.reservationId = r.id " +
            "WHERE r.sessionId IN :sessionIds AND r.status = 'CANCELLED'")
    int countCancelled(@Param("sessionIds") List<UUID> sessionIds);
}