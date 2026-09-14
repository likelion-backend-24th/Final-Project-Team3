package com.example.reservationservice.reservation.dto;

import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;

public record AttendeeInfo(AgeGroup ageGroup, Job job) {}