package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberListResponse(
        UUID id,
        String email,
        String name,
        Role role,
        LocalDateTime createdAt
) {
    public static MemberListResponse from(Member member) {
        return new MemberListResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                member.getCreatedAt()
        );
    }
}


