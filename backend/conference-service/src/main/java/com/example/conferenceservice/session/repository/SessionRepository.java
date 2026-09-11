package com.example.conferenceservice.session.repository;

import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByConferenceId(UUID conferenceId);

    List<Session> findByConferenceIdAndStatus(UUID conferenceId, SessionStatus status);

    Optional<Session> findByIdAndConference_Status(UUID id, ConferenceStatus status);

    Page<Session> findByStatus(SessionStatus status, Pageable pageable);

    // 컨퍼런스 목록 조회에서 세션 개수를 N+1 없이 한 번에 집계하기 위한 벌크 카운트 쿼리.
    @Query("SELECT s.conference.id AS conferenceId, COUNT(s) AS count "
            + "FROM Session s WHERE s.conference.id IN :conferenceIds AND s.status = :status "
            + "GROUP BY s.conference.id")
    List<ConferenceSessionCount> countByConferenceIdInAndStatus(
            @Param("conferenceIds") List<UUID> conferenceIds,
            @Param("status") SessionStatus status);

    // 주최자 본인의 컨퍼런스 목록(getMyConferences)에서는 승인 대기 세션도 본인 눈에 보여야 하므로
    // 상태와 무관하게 전체 세션 개수를 집계한다.
    @Query("SELECT s.conference.id AS conferenceId, COUNT(s) AS count "
            + "FROM Session s WHERE s.conference.id IN :conferenceIds "
            + "GROUP BY s.conference.id")
    List<ConferenceSessionCount> countByConferenceIdIn(@Param("conferenceIds") List<UUID> conferenceIds);

    interface ConferenceSessionCount {
        UUID getConferenceId();
        long getCount();
    }
}
