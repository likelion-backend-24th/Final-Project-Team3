package com.example.memberservice.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizerSignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String name,
        @NotBlank @Size(max = 100) String organizationName,
        @NotBlank String businessNo
) {}
