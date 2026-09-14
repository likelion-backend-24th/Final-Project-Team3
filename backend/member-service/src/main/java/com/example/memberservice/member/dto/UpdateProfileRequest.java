package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import jakarta.validation.constraints.NotNull;

public record UpdateProfileRequest(
        @NotNull AgeGroup ageGroup,
        @NotNull Job job
        ) {}
