package com.example.memberservice.dto;

import java.util.UUID;

public record SignupResponse(
        UUID memberId,
        String email,
        String name,
        String role
) {}
