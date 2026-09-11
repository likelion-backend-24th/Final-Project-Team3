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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpirationScheduler {

    private final ReservationRepository reservationRepository;
    private final SessionCapacityLockRepository sessionCapacityLockRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final ConferenceServiceClient conferenceServiceClient;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireOverdueHolds() {
        List<Reservation> expiredHolds = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.HOLD, LocalDateTime.now());

        for (Reservation reservation : expiredHolds) {
            reservation.markAsCancelled();
            sessionCapacityLockRepository.decrease(reservation.getSessionId(), reservation.getHeadcount());
            promoteQueueIfCapacityAvailable(reservation.getSessionId());
        }
    }

    // 만료로 반납된 좌석은 새로운 신청자가 아니라 대기열 맨 앞 순번이 먼저 가져가야 한다 (새치기 방지).
    // 반납된 좌석으로 여러 명을 승격할 수 있는 경우(headcount가 작은 대기자들)까지 고려해 반복 승격한다.
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
                // 이미 다른 경로로 처리된(취소 등) 대기열 항목 -> 정리하고 다음 순번 확인
                waitingQueueRepository.deleteByReservationId(front.getReservationId());
                continue;
            }

            int capacity;
            try {
                capacity = conferenceServiceClient.getSessionCapacity(sessionId);
            } catch (RuntimeException e) {
                log.warn("정원 확인 실패로 대기열 승격을 건너뜀. sessionId={}", sessionId, e);
                return;
            }

            int updatedRows = sessionCapacityLockRepository.tryIncrease(
                    sessionId, queuedReservation.getHeadcount(), capacity);
            if (updatedRows == 0) {
                // 남은 좌석으로는 다음 순번을 승격할 수 없음
                return;
            }

            queuedReservation.markAsHold();
            waitingQueueRepository.deleteByReservationId(queuedReservation.getId());
            waitingQueueRepository.decrementPositionAfter(sessionId, front.getPosition());
        }
    }
}
