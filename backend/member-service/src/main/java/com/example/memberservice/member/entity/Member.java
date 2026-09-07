package com.example.memberservice.member.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "member")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 100)
    private String organizationName;

    @Column(unique = true, length = 20)
    private String businessNo;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @PrePersist
    private void assignId() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }

    @Builder
    private Member(String email, String password, String name, Role role, String organizationName, String businessNo) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.organizationName = organizationName;
        this.businessNo = businessNo;
    }

    public static Member newMember(String email, String encodedPassword, String name) {
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .role(Role.MEMBER)
                .build();
    }

    public static Member newOrganizer(String email, String encodedPassword, String name, String organizationName, String businessNo) {
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .role(Role.ORGANIZER)
                .organizationName(organizationName)
                .businessNo(businessNo)
                .build();
    }
}
