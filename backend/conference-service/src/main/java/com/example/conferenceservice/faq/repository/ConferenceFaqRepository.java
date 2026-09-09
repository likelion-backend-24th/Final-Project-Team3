package com.example.conferenceservice.faq.repository;

import com.example.conferenceservice.faq.entity.ConferenceFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConferenceFaqRepository extends JpaRepository<ConferenceFaq, UUID> {
    List<ConferenceFaq> findByConferenceIdOrderByCreatedAtDesc(UUID conferenceId);
}
