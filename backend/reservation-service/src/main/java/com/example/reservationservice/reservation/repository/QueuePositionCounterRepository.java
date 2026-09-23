package com.example.reservationservice.reservation.repository;

import com.example.reservationservice.reservation.entity.QueuePositionCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface QueuePositionCounterRepository extends JpaRepository<QueuePositionCounter, UUID> {

    // 행이 없으면 next_position=1로 만들고, 있으면 그대로 둔다(session_capacity_lock.ensureExists와 동일 패턴).
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO queue_position_counter (session_id, next_position) " +
            "VALUES (:sessionId, 1) " +
            "ON DUPLICATE KEY UPDATE session_id = session_id",
            nativeQuery = true)
    void ensureExists(@Param("sessionId") UUID sessionId);

    // 이 UPDATE가 행 잠금을 잡고 커밋될 때까지 동시 호출을 직렬화하므로, 같은 호출 트랜잭션 안에서
    // 바로 이어지는 SELECT는 자신이 막 올린 값을 안전하게 읽는다(read-own-writes) — 별도 재시도가 필요 없다.
    @Modifying
    @Transactional
    @Query("UPDATE QueuePositionCounter c SET c.nextPosition = c.nextPosition + 1 WHERE c.sessionId = :sessionId")
    void increment(@Param("sessionId") UUID sessionId);

    @Query("SELECT c.nextPosition - 1 FROM QueuePositionCounter c WHERE c.sessionId = :sessionId")
    int getLastAssignedPosition(@Param("sessionId") UUID sessionId);
}
