package com.example.memberservice.auth.session.entity;

import com.example.memberservice.auth.social.SocialProvider;
import com.example.memberservice.common.BaseEntity;
import com.example.memberservice.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "social_account", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Builder
    private SocialAccount(Member member, SocialProvider provider, String providerUserId) {
        this.member = member;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    public static SocialAccount of(Member member, SocialProvider provider, String providerUserId) {
        return SocialAccount.builder().member(member).provider(provider).providerUserId(providerUserId).build();
    }
}
