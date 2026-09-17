package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "내 프로필 수정 요청")
public record UpdateProfileRequest(
        @Schema(description = "연령대")
        @NotNull AgeGroup ageGroup,

        @Schema(description = "직무")
        @NotNull Job job
        ) {}
