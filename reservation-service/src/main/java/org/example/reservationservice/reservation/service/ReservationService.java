package org.example.reservationservice.reservation.service;

import lombok.RequiredArgsConstructor;
import org.example.reservationservice.common.exception.BusinessException;
import org.example.reservationservice.reservation.dto.ReservationResult;
import org.example.reservationservice.reservation.entity.Reservation;
import org.example.reservationservice.reservation.entity.WaitingQueue;
import org.example.reservationservice.reservation.exception.ReservationErrorCode;
import org.example.reservationservice.reservation.repository.ReservationRepository;
import org.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import org.example.reservationservice.reservation.repository.WaitingQueueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final SessionCapacityLockRepository sessionCapacityLockRepository;

    @Transactional
    public ReservationResult createHoldOrQueue(UUID sessionId, UUID memberId, int headcount) {

        int capacity = getSessionCapacity(sessionId);

        // UPSERT로 Row 존재를 원자적으로 보장 (예외 발생 소지 자체가 없음)
        sessionCapacityLockRepository.ensureExists(sessionId);

        // 조건부 UPDATE로 정원 체크와 증가를 원자적으로 처리
        int updatedRows = sessionCapacityLockRepository.tryIncrease(sessionId, headcount, capacity);

        if (updatedRows == 1) {
            Reservation reservation = Reservation.builder()
                    .sessionId(sessionId)
                    .memberId(memberId)
                    .headcount(headcount)
                    .build();
            reservationRepository.save(reservation);

            return ReservationResult.hold(reservation.getId());
        }

        Reservation queuedReservation = Reservation.builder()
                .sessionId(sessionId)
                .memberId(memberId)
                .headcount(headcount)
                .build();
        queuedReservation.markAsQueued();
        reservationRepository.save(queuedReservation);

        int nextPosition = waitingQueueRepository.findMaxPositionBySessionId(sessionId) + 1;
        WaitingQueue waitingQueue = WaitingQueue.builder()
                .reservationId(queuedReservation.getId())
                .sessionId(sessionId)
                .memberId(memberId)
                .position(nextPosition)
                .build();
        waitingQueueRepository.save(waitingQueue);

        return ReservationResult.queued(queuedReservation.getId(), nextPosition);
    }

    private int getSessionCapacity(UUID sessionId) {
        return 10;
    }

    public int getQueuePosition(UUID reservationId) {
        return waitingQueueRepository.findByReservationId(reservationId)
                .map(WaitingQueue::getPosition)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));
    }
}