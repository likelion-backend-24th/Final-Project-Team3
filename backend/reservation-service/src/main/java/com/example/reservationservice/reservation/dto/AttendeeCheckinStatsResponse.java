package com.example.reservationservice.reservation.dto;

import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;

import java.util.Map;

public record AttendeeCheckinStatsResponse(
        int checkedInCount,
        Map<AgeGroup, Long> ageGroupDistribution,
        Map<Job, Long> jobDistribution
) {}