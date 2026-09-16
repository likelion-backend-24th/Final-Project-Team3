package com.example.conferenceservice.settlement.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.common.security.OwnerScopeGuard;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
import com.example.conferenceservice.settlement.client.PaymentSummaryClient;
import com.example.conferenceservice.settlement.client.PaymentSummaryClient.PaymentSummaryResponse;
import com.example.conferenceservice.settlement.client.PaymentSummaryUnavailableException;
import com.example.conferenceservice.settlement.dto.ConferenceSettlementResponse;
import com.example.conferenceservice.settlement.exception.SettlementErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 16-2 ConferenceOperationStatusService와 같은 이유로 @Transactional을 의도적으로 붙이지 않는다:
 * Reservation-Service 호출이 지연/실패하면 DB 커넥션 풀을 오래 점유하게 되기 때문.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConferenceSettlementService {

    private final ConferenceRepository conferenceRepository;
    private final SessionRepository sessionRepository;
    private final PaymentSummaryClient paymentSummaryClient;

    public ConferenceSettlementResponse getSettlement(UUID conferenceId, UUID requesterId) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
        OwnerScopeGuard.verify(requesterId, conference.getOrganizerId(), ConferenceErrorCode.CONFERENCE_ACCESS_DENIED);

        List<UUID> sessionIds = sessionRepository.findByConferenceId(conferenceId).stream()
                .map(Session::getId)
                .toList();

        PaymentSummaryResponse summary = fetchPaymentSummary(sessionIds);

        return new ConferenceSettlementResponse(
                conference.getId(),
                conference.getTitle(),
                summary.totalRevenue(),
                summary.refundedAmount(),
                summary.netRevenue(),
                summary.confirmedCount(),
                summary.cancelledCount());
    }

    private PaymentSummaryResponse fetchPaymentSummary(List<UUID> sessionIds) {
        try {
            return paymentSummaryClient.getPaymentSummary(sessionIds);
        } catch (PaymentSummaryUnavailableException e) {
            log.warn("Reservation-Service 응답 실패로 정산 조회를 중단합니다: sessionIds={}", sessionIds, e);
            throw new BusinessException(SettlementErrorCode.RESERVATION_SERVICE_UNAVAILABLE);
        }
    }
}
