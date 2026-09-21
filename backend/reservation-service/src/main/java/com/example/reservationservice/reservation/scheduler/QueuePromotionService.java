package com.example.reservationservice.reservation.scheduler;

import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QueuePromotionService {

    private final ReservationRepository reservationRepository;
    private final SessionCapacityLockRepository sessionCapacityLockRepository;
    private final WaitingQueueRepository waitingQueueRepository;

    @Transactional
    public List<UUID> expireHolds() {
        List<Reservation> expiredHolds = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.HOLD, LocalDateTime.now());

        List<UUID> sessionIds = new ArrayList<>();
        for (Reservation reservation : expiredHolds) {
            reservation.markAsCancelled();
            sessionCapacityLockRepository.decrease(reservation.getSessionId(), reservation.getHeadcount());
            sessionIds.add(reservation.getSessionId());
        }
        return sessionIds;
    }

    @Transactional
    public void deleteQueueEntry(UUID reservationId) {
        waitingQueueRepository.deleteByReservationId(reservationId);
    }

    @Transactional
    public boolean promoteOneIfCapacityAvailable(
            UUID sessionId, UUID reservationId, int headcount, int position, int capacity) {
        int updatedRows = sessionCapacityLockRepository.tryIncrease(sessionId, headcount, capacity);
        if (updatedRows == 0) {
            return false;
        }

        Reservation queuedReservation = reservationRepository.findById(reservationId).orElseThrow();
        queuedReservation.markAsHold();
        waitingQueueRepository.deleteByReservationId(reservationId);
        waitingQueueRepository.decrementPositionAfter(sessionId, position);
        return true;
    }
}