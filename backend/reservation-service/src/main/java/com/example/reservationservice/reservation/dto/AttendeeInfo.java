package com.example.reservationservice.reservation.dto;

import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동반자 1인의 ·직무 정보")
public record AttendeeInfo(
        @Schema(description = "연령대")
        AgeGroup ageGroup,

        @Schema(description = "직무")
        Job job
) {}