package com.example.conferenceservice.notice.repository;

import com.example.conferenceservice.notice.entity.ConferenceNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConferenceNoticeRepository extends JpaRepository<ConferenceNotice, UUID> {
    List<ConferenceNotice> findByConferenceIdOrderByCreatedAtDesc(UUID conferenceId);
}
