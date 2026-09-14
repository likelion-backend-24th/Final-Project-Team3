package com.example.reservationservice.reservation.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "attendee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendee {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "reservation_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID reservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Job job;

    @Builder
    public Attendee(UUID reservationId, AgeGroup ageGroup, Job job) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.reservationId = reservationId;
        this.ageGroup = ageGroup;
        this.job = job;
    }
}