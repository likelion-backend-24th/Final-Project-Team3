package com.example.memberservice.dto;

public record SignupResponse(
        Long memberId,
        String email,
        String name,
        String role
) {}
