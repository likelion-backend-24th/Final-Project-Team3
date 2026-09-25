package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberDetailResponse (
        UUID id,
        String email,
        String name,
        Role role,
        String organizationName,
        String businessNo,
        AgeGroup ageGroup,
        Job job,
        LocalDateTime createdAt
){
    public static MemberDetailResponse from(Member member) {
        return new MemberDetailResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                member.getOrganizationName(),
                member.getBusinessNo(),
                member.getAgeGroup(),
                member.getJob(),
                member.getCreatedAt()
        );
    }
}
