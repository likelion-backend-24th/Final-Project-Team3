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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Job job;

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
    private Member(String email, String password, String name, Role role, String organizationName, String businessNo, AgeGroup ageGroup, Job job) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.organizationName = organizationName;
        this.businessNo = businessNo;
        this.ageGroup = ageGroup;
        this.job = job;
    }

    public static Member newMember(String email, String encodedPassword, String name, AgeGroup ageGroup, Job job) {
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .role(Role.MEMBER)
                .ageGroup(ageGroup)
                .job(job)
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

    // 서비스가 필드를 직접 건드리지 않고 프로필 수정이라는 의미 단위로만 상태를 바꾸게 함
    // @Transactional 안에서 이 메서드를 호출하면 JPA가 변경을 감지해서 별도로 save()를 안 불러도 트랜잭션 커밋 시점에 자동으로 UPDATE 쿼리가 나감
    public void updateProfile(AgeGroup ageGroup, Job job) {
        this.ageGroup = ageGroup;
        this.job = job;
    }
}
