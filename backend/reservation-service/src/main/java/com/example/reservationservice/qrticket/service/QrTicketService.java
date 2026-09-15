package com.example.reservationservice.qrticket.service;

import com.example.reservationservice.qrticket.entity.QrTicket;
import com.example.reservationservice.qrticket.repository.QrTicketRepository;
import com.example.reservationservice.reservation.entity.Attendee;
import com.example.reservationservice.reservation.repository.AttendeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrTicketService {

    private final QrTicketRepository qrTicketRepository;
    private final AttendeeRepository attendeeRepository;

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

    private String generateQrCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
