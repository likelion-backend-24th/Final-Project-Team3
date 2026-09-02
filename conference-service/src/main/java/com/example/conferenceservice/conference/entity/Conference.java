package com.example.conferenceservice.conference.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Entity
@Table(name = "conference")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Conference {
    @Id
    private UUID id;

    // 논리적 FK - Member-Service의 organizer PK를 값으로만 보관
    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConferenceStatus status; // 신청, 승인, 반려

    @Column(nullable = false)
    private int capacity;

    @PrePersist
    private void assignId() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }


}
