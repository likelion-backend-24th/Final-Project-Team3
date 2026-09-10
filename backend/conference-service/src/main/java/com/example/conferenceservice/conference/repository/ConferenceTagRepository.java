package com.example.conferenceservice.conference.repository;

import com.example.conferenceservice.conference.entity.ConferenceTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConferenceTagRepository extends JpaRepository<ConferenceTag, UUID> {
    List<ConferenceTag> findByConferenceId(UUID conferenceId);
}
