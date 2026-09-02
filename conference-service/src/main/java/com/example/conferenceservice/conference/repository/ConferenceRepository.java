package com.example.conferenceservice.conference.repository;

import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConferenceRepository extends JpaRepository<Conference, UUID> {
    Page<Conference> findByStatus(ConferenceStatus status, Pageable pageable);

    Optional<Conference> findByIdAndStatus(UUID id, ConferenceStatus status);
}
