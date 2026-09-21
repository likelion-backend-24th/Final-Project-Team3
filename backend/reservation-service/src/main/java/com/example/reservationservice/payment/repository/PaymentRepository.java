package com.example.reservationservice.payment.repository;

import com.example.reservationservice.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByReservationId(UUID reservationId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "JOIN Reservation r ON p.reservationId = r.id " +
            "WHERE r.sessionId IN :sessionIds AND r.status = 'CONFIRMED'")
    int sumConfirmedAmount(@Param("sessionIds") List<UUID> sessionIds);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "JOIN Reservation r ON p.reservationId = r.id " +
            "WHERE r.status = 'CONFIRMED' " +
            "AND (:startDate IS NULL OR p.paidAt >= :startDate) " +
            "AND (:endDate IS NULL OR p.paidAt < :endDate)")
    long sumConfirmedAmount(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(p) FROM Payment p " +
            "JOIN Reservation r ON p.reservationId = r.id " +
            "WHERE r.sessionId IN :sessionIds AND r.status = 'CONFIRMED'")
    int countConfirmed(@Param("sessionIds") List<UUID> sessionIds);

    @Query("SELECT COALESCE(SUM(p.refundedAmount), 0) FROM Payment p " +
            "JOIN Reservation r ON p.reservationId = r.id " +
            "WHERE r.sessionId IN :sessionIds AND r.status = 'CANCELLED'")
    int sumRefundedAmount(@Param("sessionIds") List<UUID> sessionIds);

    @Query("SELECT COUNT(p) FROM Payment p " +
            "JOIN Reservation r ON p.reservationId = r.id " +
            "WHERE r.sessionId IN :sessionIds AND r.status = 'CANCELLED'")
    int countCancelled(@Param("sessionIds") List<UUID> sessionIds);

    @Query(value = "SELECT AVG(TIMESTAMPDIFF(SECOND, r.created_at, p.paid_at)) " +
            "FROM payment p JOIN reservation r ON p.reservation_id = r.id " +
            "WHERE r.status = 'CONFIRMED'", nativeQuery = true)
    Double findAveragePaymentSeconds();

    @Query(value = "SELECT AVG(TIMESTAMPDIFF(SECOND, r.created_at, p.paid_at)) " +
            "FROM payment p JOIN reservation r ON p.reservation_id = r.id " +
            "WHERE r.status = 'CONFIRMED' AND r.session_id IN :sessionIds", nativeQuery = true)
    Double findAveragePaymentSecondsBySessionIds(@Param("sessionIds") List<UUID> sessionIds);
}