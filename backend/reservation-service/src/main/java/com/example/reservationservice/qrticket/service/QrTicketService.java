package com.example.reservationservice.qrticket.service;

import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.dto.AttendeeCheckinStatsResponse;
import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Attendee;
import com.example.reservationservice.reservation.entity.Job;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.repository.AttendeeRepository;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<QrTicket> getTicketsByReservation(UUID reservationId) {
        return qrTicketRepository.findByReservationId(reservationId);
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

    private String generateQrCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}