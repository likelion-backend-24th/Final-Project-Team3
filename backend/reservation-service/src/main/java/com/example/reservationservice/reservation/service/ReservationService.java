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
import com.example.reservationservice.qrticket.exception.QrTicketErrorCode;
import com.example.reservationservice.qrticket.exception.QrTicketException;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.qrticket.service.QrTicketService;
import com.example.reservationservice.reservation.scheduler.QueuePromotionService;
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
    private final QueuePositionCounterRepository queuePositionCounterRepository;
    private final SessionCapacityLockRepository sessionCapacityLockRepository;
    private final ConferenceServiceClient conferenceServiceClient;
    private final PaymentService paymentService;
    private final PortOnePaymentVerifier portOnePaymentVerifier;
    private final QrTicketService qrTicketService;
    private final AttendeeRepository attendeeRepository;
    private final PaymentRepository paymentRepository;
    private final ActiveReservationLockRepository activeReservationLockRepository;
    private final QueuePromotionService queuePromotionService;
    private final QrTicketRepository qrTicketRepository;

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

            try {
                activeReservationLockRepository.save(
                        new ActiveReservationLock(sessionId, memberId, reservation.getId()));
            } catch (DataIntegrityViolationException e) {
                throw new BusinessException(ReservationErrorCode.DUPLICATE_RESERVATION);
            }

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

        try {
            activeReservationLockRepository.save(
                    new ActiveReservationLock(sessionId, memberId, queuedReservation.getId()));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }

        saveAttendees(queuedReservation.getId(), headcount, attendees, groupAttendee);

        WaitingQueue waitingQueue = registerToQueue(sessionId, queuedReservation.getId(), memberId);

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

    private Integer getSessionPrice(UUID sessionId) {
        try {
            return conferenceServiceClient.getSessionPrice(sessionId);
        } catch (ConferenceServiceUnavailableException e) {
            throw new BusinessException(ReservationErrorCode.CONFERENCE_SERVICE_UNAVAILABLE);
        }
    }

    public QueuePositionResponse getQueuePosition(UUID reservationId, UUID requesterId) {
        WaitingQueue queueEntry = waitingQueueRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));

        if (!queueEntry.getMemberId().equals(requesterId)) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }

        int position = queueEntry.getPosition();
        Integer estimatedWaitMinutes = calculateEstimatedWaitMinutes(queueEntry.getSessionId(), position);

        return new QueuePositionResponse(position, estimatedWaitMinutes);
    }

    // 대기자에게 자리가 나는 건 (1) 결제 대기(HOLD)가 결제 없이 만료되거나 (2) 누가 취소할 때뿐이다.
    // 결제를 마친 좌석은 취소 전까지 계속 차 있으므로, 예측할 수 있는 건 (1)뿐이다:
    // 내 순번까지 필요한 좌석 수만큼 HOLD를 만료가 빠른 순으로 쌓았을 때, 마지막으로 필요한 HOLD의 만료 시각이
    // "늦어도 이때까지는 자리가 날지 결과가 나오는" 시점이다(만료 처리 스케줄러 주기 1분 포함).
    // HOLD만으로 좌석이 모자라면(= 대부분 결제 완료) 취소를 기다려야 해서 예측할 수 없으므로 null을 돌려준다.
    private Integer calculateEstimatedWaitMinutes(UUID sessionId, int position) {
        int seatsNeeded = waitingQueueRepository.findBySessionIdAndPositionLessThanEqualOrderByPositionAsc(sessionId, position)
                .stream()
                .map(entry -> reservationRepository.findById(entry.getReservationId())
                        .map(Reservation::getHeadcount)
                        .orElse(0))
                .mapToInt(Integer::intValue)
                .sum();

        int freeSeats;
        try {
            int capacity = conferenceServiceClient.getSessionCapacity(sessionId);
            int active = sessionCapacityLockRepository.findById(sessionId)
                    .map(SessionCapacityLock::getCurrentActive)
                    .orElse(0);
            freeSeats = Math.max(capacity - active, 0);
        } catch (ConferenceServiceUnavailableException e) {
            return null;
        }

        int releasable = freeSeats;
        if (releasable >= seatsNeeded) {
            return HOLD_EXPIRATION_CHECK_MINUTES;   // 빈자리가 이미 있다 — 다음 승격 처리 때 바로 올라간다
        }
        LocalDateTime now = LocalDateTime.now();
        for (Reservation hold : reservationRepository.findBySessionIdAndStatusOrderByExpiresAtAsc(sessionId, ReservationStatus.HOLD)) {
            releasable += hold.getHeadcount();
            if (releasable >= seatsNeeded) {
                long secondsLeft = Math.max(ChronoUnit.SECONDS.between(now, hold.getExpiresAt()), 0);
                return (int) Math.ceil(secondsLeft / 60.0) + HOLD_EXPIRATION_CHECK_MINUTES;
            }
        }
        return null;
    }

    // HoldExpirationScheduler가 만료된 HOLD를 정리하고 대기열을 승격하는 주기(1분)
    private static final int HOLD_EXPIRATION_CHECK_MINUTES = 1;

    // session_capacity_lock.tryIncrease와 같은 패턴: 카운터 행에 원자적 UPDATE로 순번을 배정하므로
    // findMax+1 방식과 달리 경쟁 상태·재시도가 필요 없다(대량 동시 등록 시 재시도 소진으로 인한 실패도 없다).
    private WaitingQueue registerToQueue(UUID sessionId, UUID reservationId, UUID memberId) {
        queuePositionCounterRepository.ensureExists(sessionId);
        queuePositionCounterRepository.increment(sessionId);
        int position = queuePositionCounterRepository.getLastAssignedPosition(sessionId);

        WaitingQueue waitingQueue = WaitingQueue.builder()
                .reservationId(reservationId)
                .sessionId(sessionId)
                .memberId(memberId)
                .position(position)
                .build();
        return waitingQueueRepository.save(waitingQueue);
    }

    public boolean isQueuePositionReached(UUID reservationId) {
        WaitingQueue queueEntry = waitingQueueRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));

        return queueEntry.getPosition() == 1;
    }

    @Transactional
    public PaymentResult processPayment(UUID reservationId, UUID requesterId, String paymentId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

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
        Integer price = getSessionPrice(reservation.getSessionId());
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
                // 이미 PortOne 결제 검증까지 끝난 뒤라 실제로 돈을 받은 상태다 — 좌석을
                // 못 잡아주는데 돈만 받으면 안 되므로 여기서 바로 취소(환불) 처리한다.
                if (!isFree) {
                    portOnePaymentVerifier.cancel(paymentId, "정원 초과로 좌석 확정 실패 - 자동 환불");
                }
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
        activeReservationLockRepository.deleteByReservationId(reservationId);

        paymentService.recordPayment(reservationId, paymentMethod, expectedAmount);

        if (wasQueued && leftPosition != null) {
            queuePositionCounterRepository.decrement(reservation.getSessionId());
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
        Map<ReservationStatus, Long> counts = reservationRepository.sumHeadcountGroupByStatus(sessionId).stream()
                .collect(Collectors.toMap(
                        row -> (ReservationStatus) row[0],
                        row -> (Long) row[1]
                ));
        long holdCount = counts.getOrDefault(ReservationStatus.HOLD, 0L);
        long queuedCount = counts.getOrDefault(ReservationStatus.QUEUED, 0L);
        long confirmedCount = counts.getOrDefault(ReservationStatus.CONFIRMED, 0L);
        long cancelledCount = counts.getOrDefault(ReservationStatus.CANCELLED, 0L);
        long checkedCount = qrTicketService.countCheckedInBySessionId(sessionId);

        return new SessionStatusSummaryResponse(
                sessionId, holdCount, queuedCount, confirmedCount, cancelledCount, checkedCount);
    }

    @Transactional
    public CancelResult cancelReservation(UUID reservationId, UUID requesterId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

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

            // refundAmount > 0이면 원래 결제도 무료가 아니었다는 뜻이라(무료 세션은 amount=0으로
            // 기록됨), 이 조건만으로 PortOne에 취소할 실제 결제 건이 있는지 충분히 판별된다.
            if (refundAmount > 0) {
                portOnePaymentVerifier.cancel(
                        reservationId.toString(), refundAmount, "예약 취소 환불 (환불율 " + refundRate + "%)");
            }

            sessionCapacityLockRepository.decrease(reservation.getSessionId(), reservation.getHeadcount());
            queuePromotionService.promoteQueueIfCapacityAvailable(reservation.getSessionId());

        } else if (reservation.getStatus() == ReservationStatus.HOLD) {
            sessionCapacityLockRepository.decrease(reservation.getSessionId(), reservation.getHeadcount());
            queuePromotionService.promoteQueueIfCapacityAvailable(reservation.getSessionId());
        } else if (reservation.getStatus() == ReservationStatus.QUEUED) {
            int leftPosition = waitingQueueRepository.findByReservationId(reservationId)
                    .map(WaitingQueue::getPosition)
                    .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));
            queuePositionCounterRepository.decrement(reservation.getSessionId());
            waitingQueueRepository.deleteByReservationId(reservationId);
            waitingQueueRepository.decrementPositionAfter(reservation.getSessionId(), leftPosition);
        }

        activeReservationLockRepository.deleteByReservationId(reservationId);
        reservation.markAsCancelled();

        return CancelResult.cancelled(reservationId, refundRate, refundAmount);
    }

    // Task 12-5: 여러 명이 한 예약(headcount>1)으로 묶여 있을 때, QR 티켓 1장(=1명) 단위로 취소·부분
    // 환불한다. Attendee와 QrTicket은 서로 FK로 연결돼 있지 않아 사람 단위 식별은 QrTicket.id로 한다.
    @Transactional
    public TicketCancelResult cancelTicket(UUID reservationId, UUID ticketId, UUID requesterId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getMemberId().equals(requesterId)) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_CONFIRMED);
        }

        QrTicket ticket = qrTicketRepository.findById(ticketId)
                .filter(t -> t.getReservationId().equals(reservationId))
                .orElseThrow(() -> new QrTicketException(QrTicketErrorCode.QR_TICKET_NOT_FOUND));

        if (ticket.isUsed()) {
            throw new QrTicketException(QrTicketErrorCode.QR_TICKET_ALREADY_USED);
        }

        List<QrTicket> tickets = qrTicketRepository.findByReservationId(reservationId);
        if (tickets.size() <= 1) {
            throw new BusinessException(ReservationErrorCode.LAST_TICKET_CANNOT_BE_CANCELLED_INDIVIDUALLY);
        }

        LocalDateTime sessionStartAt = getSessionStartAt(reservation.getSessionId());
        long daysUntilStart = ChronoUnit.DAYS.between(LocalDateTime.now(), sessionStartAt);
        int refundRate = daysUntilStart >= 7 ? 100 : daysUntilStart >= 3 ? 50 : 0;

        // 1인당 가격은 결제 확정 때 쓰는 것과 같은 값(세션 가격)을 그대로 쓴다 — Payment.amount를
        // headcount로 나누면 정수 나눗셈 때문에 금액이 샐 수 있어서 쓰지 않는다.
        Integer price = getSessionPrice(reservation.getSessionId());
        int pricePerPerson = price == null ? 0 : price;
        int refundAmount = pricePerPerson * refundRate / 100;

        if (refundAmount > 0) {
            paymentService.recordRefund(reservationId, refundAmount);
            portOnePaymentVerifier.cancel(
                    reservationId.toString(), refundAmount, "예약 인원 개별 취소 환불 (환불율 " + refundRate + "%)");
        }

        qrTicketRepository.delete(ticket);
        reservation.decreaseHeadcount(1);
        sessionCapacityLockRepository.decrease(reservation.getSessionId(), 1);
        queuePromotionService.promoteQueueIfCapacityAvailable(reservation.getSessionId());

        return new TicketCancelResult(ticketId, refundRate, refundAmount, tickets.size() - 1);
    }

    private LocalDateTime getSessionStartAt(UUID sessionId) {
        try {
            return conferenceServiceClient.getSessionStartAt(sessionId);
        } catch (RestClientException e) {
            throw new BusinessException(ReservationErrorCode.CONFERENCE_SERVICE_UNAVAILABLE);
        }
    }
}