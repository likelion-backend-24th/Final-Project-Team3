package com.example.memberservice.auth.session.repository;

import com.example.memberservice.auth.session.entity.SocialAccount;
import com.example.memberservice.auth.social.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
    List<SocialAccount> findAllByMember_Id(UUID memberId);
}
