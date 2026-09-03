package com.example.memberservice.member.dto;

import java.util.UUID;

public record SignupResponse(
        UUID memberId,
        String email,
        String name,
        String role
) {}
