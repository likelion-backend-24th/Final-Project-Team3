package com.example.reservationservice.reservation.service;

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

        // 재시도 로직이 있는 안전한 메서드로 대기열 등록
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
}