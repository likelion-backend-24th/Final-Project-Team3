package com.example.conferenceservice.attendeesummary.repository;

import com.example.conferenceservice.attendeesummary.entity.ConferenceAttendeeSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConferenceAttendeeSummaryRepository extends JpaRepository<ConferenceAttendeeSummary, UUID> {
    Optional<ConferenceAttendeeSummary> findByConferenceId(UUID conferenceId);
}
