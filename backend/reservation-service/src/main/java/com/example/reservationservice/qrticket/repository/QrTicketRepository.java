package com.example.reservationservice.qrticket.repository;

import com.example.reservationservice.qrticket.entity.QrTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QrTicketRepository extends JpaRepository<QrTicket, UUID> {
    List<QrTicket> findByReservationId(UUID reservationId);
    boolean existsByReservationIdAndUsedTrue(UUID reservationId);
    List<QrTicket> findByReservationIdInAndUsedTrue(List<UUID> reservationIds);

    @Query("SELECT COUNT(q) FROM QrTicket q " +
            "JOIN Reservation r ON q.reservationId = r.id " +
            "WHERE r.sessionId = :sessionId AND q.used = true")
    long countCheckedInBySessionId(@Param("sessionId") UUID sessionId);

    Optional<QrTicket> findByCode(String code);

    // used=false인 티켓만 사용 처리하는 조건부 UPDATE. 동시에 같은 QR을 두 번 스캔해도
    // 한 요청만 1건을 갱신하고, 나머지는 updatedRows=0으로 "이미 사용됨"을 알 수 있다.
    @Modifying
    @Query("UPDATE QrTicket q SET q.used = true, q.usedAt = :usedAt WHERE q.code = :code AND q.used = false")
    int markAsUsedIfNotUsed(@Param("code") String code, @Param("usedAt") LocalDateTime usedAt);
}