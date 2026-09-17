package com.example.reservationservice.settlement.service;

import com.example.reservationservice.payment.repository.PaymentRepository;
import com.example.reservationservice.settlement.dto.SettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public SettlementResponse getSettlementDashboard(LocalDate startDate, LocalDate endDate) {
        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime to = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        long totalAmount = paymentRepository.sumConfirmedAmount(from, to);
        return new SettlementResponse(totalAmount);
    }
}
