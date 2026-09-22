package com.example.reservationservice.settlement.service;

import com.example.reservationservice.payment.repository.PaymentRepository;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.settlement.dto.SettlementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private QrTicketRepository qrTicketRepository;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(paymentRepository, reservationRepository, qrTicketRepository);
    }

    @Test
    void getSettlementDashboard_noDateFilter_passesNullBounds() {
        when(paymentRepository.sumConfirmedAmount(null, null)).thenReturn(50000L);

        SettlementResponse response = settlementService.getSettlementDashboard(null, null);

        assertThat(response.totalAmount()).isEqualTo(50000L);
        verify(paymentRepository).sumConfirmedAmount(null, null);
    }

    @Test
    void getSettlementDashboard_withDateRange_convertsToInclusiveDayBounds() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 9, 30);
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(paymentRepository.sumConfirmedAmount(any(), any())).thenReturn(10000L);

        settlementService.getSettlementDashboard(startDate, endDate);

        verify(paymentRepository).sumConfirmedAmount(fromCaptor.capture(), toCaptor.capture());
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
        assertThat(toCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
    }

    @Test
    void getSettlementDashboard_noConfirmedPayments_returnsZero() {
        when(paymentRepository.sumConfirmedAmount(null, null)).thenReturn(0L);

        SettlementResponse response = settlementService.getSettlementDashboard(null, null);

        assertThat(response.totalAmount()).isZero();
    }
}
