package com.example.conferenceservice.attendeesummary.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conference_attendee_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConferenceAttendeeSummary {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID conferenceId;

    @Column(nullable = false)
    private int checkedInCount;

    @Column(columnDefinition = "TEXT")
    private String ageGroupDistributionJson;

    @Column(columnDefinition = "TEXT")
    private String jobDistributionJson;

    @Column(columnDefinition = "TEXT")
    private String summaryText;

    @Column(nullable = false)
    private Instant generatedAt;

    @Builder
    private ConferenceAttendeeSummary(UUID conferenceId, int checkedInCount, String ageGroupDistributionJson, String jobDistributionJson, String summaryText, Instant generatedAt) {
        this.conferenceId = conferenceId;
        this.checkedInCount = checkedInCount;
        this.ageGroupDistributionJson = ageGroupDistributionJson;
        this.jobDistributionJson = jobDistributionJson;
        this.summaryText = summaryText;
        this.generatedAt = generatedAt;
    }

    @PrePersist
    private void prePersist() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }

    public void update(int checkedInCount, String ageGroupDistributionJson, String jobDistributionJson, String summaryText) {
        this.checkedInCount = checkedInCount;
        this.ageGroupDistributionJson = ageGroupDistributionJson;
        this.jobDistributionJson = jobDistributionJson;
        this.summaryText = summaryText;
        this.generatedAt = Instant.now();
    }



}
