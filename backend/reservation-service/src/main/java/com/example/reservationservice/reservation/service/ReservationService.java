package com.example.reservationservice.reservation.service;

import com.example.reservationservice.reservation.entity.ReservationStatus;
import lombok.RequiredArgsConstructor;
import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.dto.ReservationResult;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.WaitingQueue;
import com.example.reservationservice.reservation.exception.ConferenceServiceUnavailableException;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.repository.WaitingQueueRepository;
import org.hibernate.query.sql.internal.ParameterRecognizerImpl;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final SessionCapacityLockRepository sessionCapacityLockRepository;
    private final ConferenceServiceClient conferenceServiceClient;

    @Transactional
    public ReservationResult createHoldOrQueue(UUID sessionId, UUID memberId, int headcount) {

        // 중복 신청 방지: 이미 HOLD 또는 QUEUED 상태로 신청한 이력이 있는지 확인
        boolean alreadyReserved = reservationRepository.existsBySessionIdAndMemberIdAndStatusIn(
                sessionId, memberId, List.of(ReservationStatus.HOLD, ReservationStatus.QUEUED));

        if (alreadyReserved) {
            throw new BusinessException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }

        int capacity = getSessionCapacity(sessionId);

        sessionCapacityLockRepository.ensureExists(sessionId);

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

        WaitingQueue waitingQueue = registerToQueueWithRetry(sessionId, queuedReservation.getId(), memberId);

        return ReservationResult.queued(queuedReservation.getId(), waitingQueue.getPosition());
    }

    private int getSessionCapacity(UUID sessionId) {
        try {
            return conferenceServiceClient.getSessionCapacity(sessionId);
        } catch (ConferenceServiceUnavailableException e) {
            throw new BusinessException(ReservationErrorCode.CONFERENCE_SERVICE_UNAVAILABLE);
        }
    }

    public int getQueuePosition(UUID reservationId) {
        return waitingQueueRepository.findByReservationId(reservationId)
                .map(WaitingQueue::getPosition)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));
    }

    private WaitingQueue registerToQueueWithRetry(UUID sessionId, UUID reservationId, UUID memberId) {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                int nextPosition = waitingQueueRepository.findMaxPositionBySessionId(sessionId) + 1;
                WaitingQueue waitingQueue = WaitingQueue.builder()
                        .reservationId(reservationId)
                        .sessionId(sessionId)
                        .memberId(memberId)
                        .position(nextPosition)
                        .build();
                return waitingQueueRepository.saveAndFlush(waitingQueue);
            } catch (DataIntegrityViolationException e) {
                // 순번 충돌 -> 다음 순번으로 재시도
            }
        }
        throw new IllegalStateException("대기열 등록 재시도 초과");
    }

    public boolean isQueuePositionReached(UUID reservationId) {
        WaitingQueue queueEntry = waitingQueueRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));

        return queueEntry.getPosition() == 1;
    }
}