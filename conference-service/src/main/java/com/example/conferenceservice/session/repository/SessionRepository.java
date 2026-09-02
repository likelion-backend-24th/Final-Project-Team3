package com.example.conferenceservice.session.repository;

import com.example.conferenceservice.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByConferenceId(Long conferenceId);
}
