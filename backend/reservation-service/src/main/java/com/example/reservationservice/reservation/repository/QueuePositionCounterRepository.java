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

    // 대기열에서 누가 빠지면(취소·승격·결제) 뒷사람 순번을 한 칸씩 당기므로, 다음에 줄 순번도 한 칸 당긴다.
    // 안 그러면 1번이 빠진 뒤 새로 들어온 사람이 앞에 아무도 없는데 2번을 받는다.
    // 빠지는 쪽 트랜잭션에서 순번 당기기보다 먼저 호출해 카운터 행을 잠가야, 그 사이 끼어든 등록이
    // 당겨지기 전 값을 받아 순번이 겹치는 일이 없다.
    @Modifying
    @Transactional
    @Query("UPDATE QueuePositionCounter c SET c.nextPosition = c.nextPosition - 1 " +
            "WHERE c.sessionId = :sessionId AND c.nextPosition > 1")
    void decrement(@Param("sessionId") UUID sessionId);

    @Query("SELECT c.nextPosition - 1 FROM QueuePositionCounter c WHERE c.sessionId = :sessionId")
    int getLastAssignedPosition(@Param("sessionId") UUID sessionId);
}
