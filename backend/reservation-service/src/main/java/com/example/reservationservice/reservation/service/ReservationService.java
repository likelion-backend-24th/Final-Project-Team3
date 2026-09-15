package com.example.reservationservice.reservation.service;

import com.example.reservationservice.reservation.dto.*;
import com.example.reservationservice.reservation.entity.*;
import com.example.reservationservice.reservation.repository.*;
import lombok.RequiredArgsConstructor;
import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.exception.ConferenceServiceUnavailableException;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final SessionCapacityLockRepository sessionCapacityLockRepository;
    private final ConferenceServiceClient conferenceServiceClient;
    private final QrTicketRepository qrTicketRepository;
    private final PaymentRepository paymentRepository;
    private final AttendeeRepository attendeeRepository;

    @Transactional
    public ReservationResult createHoldOrQueue(
            UUID sessionId, UUID memberId, int headcount,
            List<AttendeeInfo> attendees, AttendeeInfo groupAttendee) {

        // 동반자 정보 검증
        if (headcount <= 9) {
            if (attendees == null || attendees.size() != headcount) {
                throw new BusinessException(ReservationErrorCode.ATTENDEE_INFO_REQUIRED);
            }
            for (AttendeeInfo attendee : attendees) {
                if (attendee.ageGroup() == null || attendee.job() == null) {
                    throw new BusinessException(ReservationErrorCode.ATTENDEE_INFO_REQUIRED);
                }
            }
        } else {
            if (groupAttendee == null || groupAttendee.ageGroup() == null || groupAttendee.job() == null) {
                throw new BusinessException(ReservationErrorCode.ATTENDEE_INFO_REQUIRED);
            }
        }

        // 중복 신청 방지
        boolean alreadyReserved = reservationRepository.existsBySessionIdAndMemberIdAndStatusIn(
                sessionId, memberId, List.of(ReservationStatus.HOLD, ReservationStatus.QUEUED));

        if (alreadyReserved) {
            throw new BusinessException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }

        int capacity = getSessionCapacity(sessionId);

        if (headcount > capacity) {
            throw new BusinessException(ReservationErrorCode.SESSION_CAPACITY_EXCEEDED);
        }

        sessionCapacityLockRepository.ensureExists(sessionId);

        int updatedRows = sessionCapacityLockRepository.tryIncrease(sessionId, headcount, capacity);

        if (updatedRows == 1) {
            Reservation reservation = Reservation.builder()
                    .sessionId(sessionId)
                    .memberId(memberId)
                    .headcount(headcount)
                    .build();
            reservationRepository.save(reservation);

            saveAttendees(reservation.getId(), headcount, attendees, groupAttendee);

            return ReservationResult.hold(reservation.getId());
        }

        Reservation queuedReservation = Reservation.builder()
                .sessionId(sessionId)
                .memberId(memberId)
                .headcount(headcount)
                .build();
        queuedReservation.markAsQueued();
        reservationRepository.save(queuedReservation);

        saveAttendees(queuedReservation.getId(), headcount, attendees, groupAttendee);

        WaitingQueue waitingQueue = registerToQueueWithRetry(sessionId, queuedReservation.getId(), memberId);

        return ReservationResult.queued(queuedReservation.getId(), waitingQueue.getPosition());
    }

    private void saveAttendees(UUID reservationId, int headcount,
                               List<AttendeeInfo> attendees, AttendeeInfo groupAttendee) {
        if (headcount <= 9) {
            for (AttendeeInfo info : attendees) {
                attendeeRepository.save(Attendee.builder()
                        .reservationId(reservationId)
                        .ageGroup(info.ageGroup())
                        .job(info.job())
                        .build());
            }
        } else {
            for (int i = 0; i < headcount; i++) {
                attendeeRepository.save(Attendee.builder()
                        .reservationId(reservationId)
                        .ageGroup(groupAttendee.ageGroup())
                        .job(groupAttendee.job())
                        .build());
            }
        }
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

    @Transactional
    public PaymentResult processPayment(UUID reservationId, String paymentMethod, int amount) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));


       boolean wasQueued = reservation.getStatus() == ReservationStatus.QUEUED;
        Integer leftPosition = null;

        if (wasQueued) {
            boolean reached = isQueuePositionReached(reservationId);
            if (!reached) {
                throw new BusinessException(ReservationErrorCode.QUEUE_POSITION_NOT_REACHED);
            }

            // 대기열 순번 도달 여부와 별개로, 실제 좌석이 비어있는지 원자적으로 재검증한다.
            // (그렇지 않으면 앞선 HOLD가 아직 살아있어도 대기열 1번이 결제를 통과해 정원을 초과함)
            int capacity = getSessionCapacity(reservation.getSessionId());
            int capacityUpdatedRows = sessionCapacityLockRepository.tryIncrease(
                    reservation.getSessionId(), reservation.getHeadcount(), capacity);
            if (capacityUpdatedRows == 0) {
                throw new BusinessException(ReservationErrorCode.SESSION_CAPACITY_EXCEEDED);
            }

            leftPosition = waitingQueueRepository.findByReservationId(reservationId)
                    .map(WaitingQueue::getPosition)
                    .orElse(null);
        }

        // 조건부 UPDATE로 동시 결제 요청 방어
        int updatedRows = reservationRepository.confirmIfNotAlready(reservationId);
        if (updatedRows == 0) {
            throw new BusinessException(ReservationErrorCode.ALREADY_CONFIRMED);
        }

        Payment payment = Payment.builder()
                .reservationId(reservationId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .build();
        paymentRepository.save(payment);


        if (wasQueued && leftPosition != null) {
            waitingQueueRepository.deleteByReservationId(reservationId);
            waitingQueueRepository.decrementPositionAfter(reservation.getSessionId(), leftPosition);
        }

        List<Attendee> attendees = attendeeRepository.findByReservationId(reservationId);

        List<QrTicket> tickets = new ArrayList<>();
        for (Attendee attendee : attendees) {
            QrTicket ticket = QrTicket.builder()
                    .reservationId(reservationId)
                    .code(generateQrCode())
                    .ageGroup(attendee.getAgeGroup())
                    .job(attendee.getJob())
                    .build();
            qrTicketRepository.save(ticket);
            tickets.add(ticket);
        }

        return PaymentResult.confirmed(reservationId, tickets.size());
    }

    public List<QrTicket> getQrTickets(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(ReservationErrorCode.PAYMENT_NOT_COMPLETED);
        }

        return qrTicketRepository.findByReservationId(reservationId);
    }

    public List<MyReservationResponse> getMyReservations(UUID memberId) {
        return reservationRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(MyReservationResponse::from)
                .toList();
    }

    public SessionCapacityStatusResponse getCapacityStatus(UUID sessionId) {
        int capacity = getSessionCapacity(sessionId);
        int confirmedCount = sessionCapacityLockRepository.findById(sessionId)
                .map(SessionCapacityLock::getCurrentActive)
                .orElse(0);
        int remaining = capacity - confirmedCount;
        return new SessionCapacityStatusResponse(sessionId, capacity, confirmedCount, remaining);
    }

    private String generateQrCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public PaymentSummaryResponse getPaymentSummary(List<UUID> sessionIds) {
        int totalRevenue = paymentRepository.sumConfirmedAmount(sessionIds);
        int confirmedCount = paymentRepository.countConfirmed(sessionIds);
        int refundedAmount = paymentRepository.sumRefundedAmount(sessionIds);
        int cancelledCount = paymentRepository.countCancelled(sessionIds);
        int netRevenue = totalRevenue - refundedAmount;

        return new PaymentSummaryResponse(totalRevenue, refundedAmount, netRevenue, confirmedCount, cancelledCount);
    }

    public SessionStatusSummaryResponse getStatusSummary(UUID sessionId) {
        long holdCount = reservationRepository.countBySessionIdAndStatus(sessionId, ReservationStatus.HOLD);
        long queuedCount = reservationRepository.countBySessionIdAndStatus(sessionId, ReservationStatus.QUEUED);
        long confirmedCount = reservationRepository.countBySessionIdAndStatus(sessionId, ReservationStatus.CONFIRMED);
        long cancelledCount = reservationRepository.countBySessionIdAndStatus(sessionId, ReservationStatus.CANCELLED);
        long checkedCount = qrTicketRepository.countCheckedInBySessionId(sessionId);

        return new SessionStatusSummaryResponse(
                sessionId, holdCount, queuedCount, confirmedCount, cancelledCount, checkedCount);
    }
}