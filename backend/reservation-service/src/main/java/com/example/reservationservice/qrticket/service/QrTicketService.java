package com.example.reservationservice.qrticket.service;

import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.qrticket.dto.QrTicketScanResponse;
import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.exception.QrTicketErrorCode;
import com.example.reservationservice.qrticket.exception.QrTicketException;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.dto.AttendeeCheckinStatsResponse;
import com.example.reservationservice.reservation.entity.*;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import com.example.reservationservice.reservation.repository.AttendeeRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QrTicketService {

    private final QrTicketRepository qrTicketRepository;
    private final AttendeeRepository attendeeRepository;
    private final ReservationRepository reservationRepository;
    private final ConferenceServiceClient conferenceServiceClient;

    @Transactional
    public List<QrTicket> issueTickets(UUID reservationId) {
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
        return tickets;
    }

    public long countCheckedInBySessionId(UUID sessionId) {
        return qrTicketRepository.countCheckedInBySessionId(sessionId);
    }

    public AttendeeCheckinStatsResponse getAttendeeCheckinStats(List<UUID> sessionIds) {
        List<UUID> reservationIds = reservationRepository.findBySessionIdIn(sessionIds).stream()
                .map(Reservation::getId)
                .toList();

        List<QrTicket> checkedInTickets = qrTicketRepository.findByReservationIdInAndUsedTrue(reservationIds);

        Map<AgeGroup, Long> ageGroupDistribution = checkedInTickets.stream()
                .collect(Collectors.groupingBy(QrTicket::getAgeGroup, Collectors.counting()));
        Map<Job, Long> jobDistribution = checkedInTickets.stream()
                .collect(Collectors.groupingBy(QrTicket::getJob, Collectors.counting()));

        return new AttendeeCheckinStatsResponse(checkedInTickets.size(), ageGroupDistribution, jobDistribution);
    }

    public List<QrTicket> getConfirmedTickets(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_IN_QUEUE));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(ReservationErrorCode.PAYMENT_NOT_COMPLETED);
        }

        return qrTicketRepository.findByReservationId(reservationId);
    }

    @Transactional
    public QrTicketScanResponse scan(String code) {
        QrTicket ticket = qrTicketRepository.findByCode(code)
                .orElseThrow(() -> new QrTicketException(QrTicketErrorCode.QR_TICKET_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(ticket.getReservationId())
                        .orElseThrow(() -> new QrTicketException(QrTicketErrorCode.QR_TICKET_NOT_FOUND));

        LocalDateTime sessionStartAt = conferenceServiceClient.getSessionStartAt(reservation.getSessionId());
        if (LocalDateTime.now().isBefore(sessionStartAt)) {
            throw new QrTicketException(QrTicketErrorCode.SESSION_NOT_STARTED);
        }

        ticket.scan();

        return QrTicketScanResponse.from(ticket);
    }

    private String generateQrCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}