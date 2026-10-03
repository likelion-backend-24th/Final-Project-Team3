package com.example.reservationservice.reservation.scheduler;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.entity.WaitingQueue;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.QueuePositionCounterRepository;
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
    private final QueuePositionCounterRepository queuePositionCounterRepository;
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

    // 이미 대기 상태가 아닌(취소 등) 예약의 대기열 항목 정리 — 다른 이탈과 똑같이 뒷사람 순번과 다음 순번을 당긴다
    @Transactional
    public void deleteQueueEntry(WaitingQueue entry) {
        queuePositionCounterRepository.decrement(entry.getSessionId());
        waitingQueueRepository.deleteByReservationId(entry.getReservationId());
        waitingQueueRepository.decrementPositionAfter(entry.getSessionId(), entry.getPosition());
    }

    @Transactional
    public boolean promoteOneIfCapacityAvailable(
            UUID sessionId, UUID reservationId, int headcount, int position, int capacity) {

        // 1. 먼저 정원 잠금 증가를 시도합니다. (정원이 초과되었다면 여기서 바로 실패 처리)
        int capacityIncreased = sessionCapacityLockRepository.tryIncrease(sessionId, headcount, capacity);
        if (capacityIncreased == 0) {
            return false; // 정원 초과로 승격 불가
        }

        // 2. 정원 증가가 성공했을 때만 예약 상태를 'HOLD'로 안전하게 전이합니다.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(10);
        int updatedRows = reservationRepository.promoteToHoldIfQueued(reservationId, expiresAt, now);

        if (updatedRows == 0) {
            // 만약 동시성 문제로 상태 전이가 실패했다면, 위에서 늘렸던 정원 카운터를 다시 복구(decrease)해야 합니다.
            sessionCapacityLockRepository.decrease(sessionId, headcount);
            return false;
        }

        // 3. 대기열 정리 및 순번 당기기
        queuePositionCounterRepository.decrement(sessionId);
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
                deleteQueueEntry(front);
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
                    sessionId,
                    queuedReservation.getId(),
                    queuedReservation.getHeadcount(),
                    front.getPosition(),
                    capacity);
            if (!promoted) {
                return;
            }
        }
    }
}