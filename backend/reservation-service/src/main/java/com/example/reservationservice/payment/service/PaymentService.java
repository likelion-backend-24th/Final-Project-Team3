package com.example.reservationservice.payment.service;

import com.example.reservationservice.payment.dto.PaymentSummaryResponse;
import com.example.reservationservice.payment.entity.Payment;
import com.example.reservationservice.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment recordPayment(UUID reservationId, String paymentMethod, int amount) {
        Payment payment = Payment.builder()
                .reservationId(reservationId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .build();
        return paymentRepository.save(payment);
    }

    public PaymentSummaryResponse getPaymentSummary(List<UUID> sessionIds) {
        int totalRevenue = paymentRepository.sumConfirmedAmount(sessionIds);
        int confirmedCount = paymentRepository.countConfirmed(sessionIds);
        int refundedAmount = paymentRepository.sumRefundedAmount(sessionIds);
        int cancelledCount = paymentRepository.countCancelled(sessionIds);
        int netRevenue = totalRevenue - refundedAmount;

        return new PaymentSummaryResponse(totalRevenue, refundedAmount, netRevenue, confirmedCount, cancelledCount);
    }
}
