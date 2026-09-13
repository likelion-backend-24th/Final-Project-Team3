package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;

import java.util.UUID;

public record MemberProfileResponse(
        UUID memberId,
        String email,
        String name,
        AgeGroup ageGroup,
        Job job
) {}
