package com.example.reservationservice.reservation.service;

import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.payment.repository.PaymentRepository;
import com.example.reservationservice.reservation.dto.*;
import com.example.reservationservice.reservation.entity.*;
import com.example.reservationservice.reservation.repository.*;
import com.example.reservationservice.payment.dto.PaymentResult;
import com.example.reservationservice.payment.service.PaymentService;
import com.example.reservationservice.payment.service.PortOnePaymentVerifier;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.service.QrTicketService;
import lombok.RequiredArgsConstructor;
import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.exception.ConferenceServiceUnavailableException;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final SessionCapacityLockRepository sessionCapacityLockRepository;
    private final ConferenceServiceClient conferenceServiceClient;
    private final PaymentService paymentService;
    private final PortOnePaymentVerifier portOnePaymentVerifier;
    private final QrTicketService qrTicketService;
    private final AttendeeRepository attendeeRepository;
    private final PaymentRepository paymentRepository;

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
    public PaymentResult processPayment(UUID reservationId, UUID requesterId, String paymentId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));

        if (!reservation.getMemberId().equals(requesterId)) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }

        return confirmWithVerifiedPayment(reservation, paymentId);
    }

    // PortOne 웹훅에서 재사용 — 소유자 검증은 스킵한다(웹훅은 requesterId가 없고, 이미 서명으로 신뢰됨).
    // 이미 처리된 웹훅(재시도)이 ALREADY_CONFIRMED로 걸리는 건 정상 상황이라 조용히 무시한다.
    @Transactional
    public void confirmFromWebhook(UUID reservationId, String paymentId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return;
        }
        try {
            confirmWithVerifiedPayment(reservation, paymentId);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ReservationErrorCode.ALREADY_CONFIRMED) {
                throw e;
            }
        }
    }

    private PaymentResult confirmWithVerifiedPayment(Reservation reservation, String paymentId) {
        UUID reservationId = reservation.getId();

        // 락을 잡기 전에 외부 API(PortOne) 검증부터 끝낸다 — 좌석 락을 쥔 채로 외부 호출을 기다리면 안 됨
        Integer price = conferenceServiceClient.getSessionPrice(reservation.getSessionId());
        int expectedAmount = (price == null ? 0 : price) * reservation.getHeadcount();
        // 무료 세션(price가 명시적으로 0)만 PortOne 조회를 건너뛴다. price가 null인 경우
        // (마이그레이션 없이 컬럼만 추가돼 값이 비어있는 legacy row)는 무료로 간주하지 않고
        // 그대로 verify()에 태워서 fail-closed로 막는다 — 그래야 위조된 paymentId로
        // 아무 결제 검증 없이 확정되는 걸 막을 수 있다.
        boolean isFree = price != null && price == 0;
        String paymentMethod = isFree
                ? "FREE"
                : portOnePaymentVerifier.verify(paymentId, expectedAmount).paymentMethod();

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

        paymentService.recordPayment(reservationId, paymentMethod, expectedAmount);

        if (wasQueued && leftPosition != null) {
            waitingQueueRepository.deleteByReservationId(reservationId);
            waitingQueueRepository.decrementPositionAfter(reservation.getSessionId(), leftPosition);
        }

        List<QrTicket> tickets = qrTicketService.issueTickets(reservationId);

        return PaymentResult.confirmed(reservationId, tickets.size());
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

    public AttendeeCheckinStatsResponse getAttendeeCheckinStats(List<UUID> sessionIds) {
        return qrTicketService.getAttendeeCheckinStats(sessionIds);
    }

    public SessionStatusSummaryResponse getStatusSummary(UUID sessionId) {
        // 예약 건수가 아니라 인원 수 기준으로 세야 checkedCount(QR 발급 수=1인당 1장)와 단위가 맞는다.
        long holdCount = reservationRepository.sumHeadcountBySessionIdAndStatus(sessionId, ReservationStatus.HOLD);
        long queuedCount = reservationRepository.sumHeadcountBySessionIdAndStatus(sessionId, ReservationStatus.QUEUED);
        long confirmedCount = reservationRepository.sumHeadcountBySessionIdAndStatus(sessionId, ReservationStatus.CONFIRMED);
        long cancelledCount = reservationRepository.sumHeadcountBySessionIdAndStatus(sessionId, ReservationStatus.CANCELLED);
        long checkedCount = qrTicketService.countCheckedInBySessionId(sessionId);

        return new SessionStatusSummaryResponse(
                sessionId, holdCount, queuedCount, confirmedCount, cancelledCount, checkedCount);
    }

    @Transactional
    public CancelResult cancelReservation(UUID reservationId, UUID requesterId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));

        if (!reservation.getMemberId().equals(requesterId)) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(ReservationErrorCode.ALREADY_CANCELLED);
        }

        Integer refundRate = null;
        Integer refundAmount = null;

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            LocalDateTime sessionStartAt = getSessionStartAt(reservation.getSessionId());
            long daysUntilStart = ChronoUnit.DAYS.between(LocalDateTime.now(), sessionStartAt);

            if (daysUntilStart >= 7) {
                refundRate = 100;
            } else if (daysUntilStart >= 3) {
                refundRate = 50;
            } else {
                refundRate = 0;
            }

            int originalAmount = paymentRepository.findByReservationId(reservationId)
                    .map(Payment::getAmount)
                    .orElse(0);
            refundAmount = originalAmount * refundRate / 100;

            paymentService.recordRefund(reservationId, refundAmount);

            sessionCapacityLockRepository.decrease(reservation.getSessionId(), reservation.getHeadcount());

        } else if (reservation.getStatus() == ReservationStatus.HOLD) {
            sessionCapacityLockRepository.decrease(reservation.getSessionId(), reservation.getHeadcount());
        } else if (reservation.getStatus() == ReservationStatus.QUEUED) {
            int leftPosition = waitingQueueRepository.findByReservationId(reservationId)
                    .map(WaitingQueue::getPosition)
                    .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));
            waitingQueueRepository.deleteByReservationId(reservationId);
            waitingQueueRepository.decrementPositionAfter(reservation.getSessionId(), leftPosition);
        }

        reservation.markAsCancelled();

        return CancelResult.cancelled(reservationId, refundRate, refundAmount);
    }

    private LocalDateTime getSessionStartAt(UUID sessionId) {
        try {
            return conferenceServiceClient.getSessionStartAt(sessionId);
        } catch (RestClientException e) {
            throw new BusinessException(ReservationErrorCode.CONFERENCE_SERVICE_UNAVAILABLE);
        }
    }
}