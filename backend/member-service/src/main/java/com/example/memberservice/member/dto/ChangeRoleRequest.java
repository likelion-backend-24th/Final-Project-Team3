package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

@NotNull
public record ChangeRoleRequest(Role role) {}