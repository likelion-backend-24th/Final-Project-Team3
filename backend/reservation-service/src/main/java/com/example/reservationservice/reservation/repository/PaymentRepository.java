package com.example.reservationservice.reservation.repository;

import com.example.reservationservice.reservation.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
}