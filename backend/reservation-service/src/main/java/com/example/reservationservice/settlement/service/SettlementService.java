package com.example.reservationservice.settlement.service;

import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.payment.repository.PaymentRepository;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.settlement.dto.SettlementDetailResponse;
import com.example.reservationservice.settlement.dto.SettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final QrTicketRepository qrTicketRepository;

    @Transactional(readOnly = true)
    public SettlementResponse getSettlementDashboard(LocalDate startDate, LocalDate endDate) {
        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime to = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        long totalAmount = paymentRepository.sumConfirmedAmount(from, to);
        return new SettlementResponse(totalAmount);
    }

    // 관리자 화면 전용, 트래픽이 크지 않은 페이지라 건당 예약/티켓 조회(N+1)를 그대로 둔다.
    // ponytail: 목록이 커지면 join 쿼리로 바꾸되, 지금은 페이지당 20~50건 수준이라 불필요.
    @Transactional(readOnly = true)
    public Page<SettlementDetailResponse> getSettlementDetails(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime to = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        Page<Payment> payments = paymentRepository.findAllByPaidAtBetween(from, to, pageable);
        return payments.map(this::toDetail);
    }

    private SettlementDetailResponse toDetail(Payment payment) {
        Reservation reservation = reservationRepository.findById(payment.getReservationId())
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        List<QrTicket> tickets = qrTicketRepository.findByReservationId(reservation.getId());
        int checkedInCount = (int) tickets.stream().filter(QrTicket::isUsed).count();
        return SettlementDetailResponse.of(payment, reservation, tickets.size(), checkedInCount);
    }
}
