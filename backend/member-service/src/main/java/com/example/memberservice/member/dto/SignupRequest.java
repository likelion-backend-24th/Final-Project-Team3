package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String name,
        @NotNull AgeGroup ageGroup,
        @NotNull Job job
){}
