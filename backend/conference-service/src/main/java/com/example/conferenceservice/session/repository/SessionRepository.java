package com.example.conferenceservice.session.repository;

import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByConferenceId(UUID conferenceId);

    Optional<Session> findByIdAndConference_Status(UUID id, ConferenceStatus status);

    Page<Session> findByStatus(SessionStatus status, Pageable pageable);
}
