package com.example.conferenceservice.faq.entity;

import com.example.conferenceservice.conference.entity.Conference;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Entity
@Table(name = "conference_faq")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceFaq {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    @Column(nullable = false)
    private String question;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.updatedAt = LocalDateTime.now();
    }
}
