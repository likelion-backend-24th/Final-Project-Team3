package com.example.reservationservice.reservation.scheduler;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.entity.WaitingQueue;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueuePromotionService {

    private final ReservationRepository reservationRepository;
    private final SessionCapacityLockRepository sessionCapacityLockRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final ConferenceServiceClient conferenceServiceClient;

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

    // 대기열 맨 앞부터, 정원이 허용하는 한 계속 승격한다.
    // HoldExpirationScheduler(HOLD 만료)와 ReservationService.cancelReservation(능동 취소)
    // 양쪽에서 공통으로 호출한다.
    @Transactional

    public void promoteQueueIfCapacityAvailable(UUID sessionId) {
        while (true) {
            WaitingQueue front = waitingQueueRepository.findFirstBySessionIdOrderByPositionAsc(sessionId)
                    .orElse(null);
            if (front == null) {
                return;
            }

            Reservation queuedReservation = reservationRepository.findById(front.getReservationId())
                    .orElse(null);
            if (queuedReservation == null || queuedReservation.getStatus() != ReservationStatus.QUEUED) {
                deleteQueueEntry(front.getReservationId());
                continue;
            }

            int capacity;
            try {
                capacity = conferenceServiceClient.getSessionCapacity(sessionId);
            } catch (RuntimeException e) {
                log.warn("정원 확인 실패로 대기열 승격을 건너뜀. sessionId={}", sessionId, e);
                return;
            }

            boolean promoted = promoteOneIfCapacityAvailable(
                    sessionId, queuedReservation.getId(), queuedReservation.getHeadcount(),
                    front.getPosition(), capacity);
            if (!promoted) {
                return;
            }
        }
    }
}