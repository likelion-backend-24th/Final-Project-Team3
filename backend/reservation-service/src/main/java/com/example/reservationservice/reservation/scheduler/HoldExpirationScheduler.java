package com.example.reservationservice.reservation.scheduler;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.entity.WaitingQueue;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpirationScheduler {

    private final ReservationRepository reservationRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final ConferenceServiceClient conferenceServiceClient;
    private final QueuePromotionService queuePromotionService;

    @Scheduled(fixedRate = 60000)
    public void expireOverdueHolds() {
        List<UUID> affectedSessionIds = queuePromotionService.expireHolds();

        for (UUID sessionId : affectedSessionIds) {
            promoteQueueIfCapacityAvailable(sessionId);
        }
    }

    private void promoteQueueIfCapacityAvailable(UUID sessionId) {
        while (true) {
            WaitingQueue front = waitingQueueRepository.findFirstBySessionIdOrderByPositionAsc(sessionId)
                    .orElse(null);
            if (front == null) {
                return;
            }

            Reservation queuedReservation = reservationRepository.findById(front.getReservationId())
                    .orElse(null);
            if (queuedReservation == null || queuedReservation.getStatus() != ReservationStatus.QUEUED) {
                queuePromotionService.deleteQueueEntry(front.getReservationId());
                continue;
            }

            int capacity;
            try {
                capacity = conferenceServiceClient.getSessionCapacity(sessionId);
            } catch (RuntimeException e) {
                log.warn("정원 확인 실패로 대기열 승격을 건너뜀. sessionId={}", sessionId, e);
                return;
            }

            boolean promoted = queuePromotionService.promoteOneIfCapacityAvailable(
                    sessionId, queuedReservation.getId(), queuedReservation.getHeadcount(),
                    front.getPosition(), capacity);
            if (!promoted) {
                return;
            }
        }
    }
}