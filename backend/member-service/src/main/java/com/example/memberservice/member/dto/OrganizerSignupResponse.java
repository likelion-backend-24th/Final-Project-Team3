package com.example.memberservice.member.dto;

import java.util.UUID;

public record OrganizerSignupResponse(
        UUID memberId,
        String email,
        String name,
        String organizationName,
        String businessNo,
        String role
) {}
