package com.example.reservationservice.reservation.scheduler;

import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HoldExpirationScheduler {

    private final ReservationRepository reservationRepository;
    private final SessionCapacityLockRepository sessionCapacityLockRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireOverdueHolds() {
        List<Reservation> expiredHolds = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.HOLD, LocalDateTime.now());

        for (Reservation reservation : expiredHolds) {
            reservation.markAsCancelled();
            sessionCapacityLockRepository.decrease(reservation.getSessionId(), reservation.getHeadcount());
        }
    }
}
