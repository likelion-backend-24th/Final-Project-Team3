package com.example.memberservice.auth.repository;

import com.example.memberservice.auth.entity.SocialAccount;
import com.example.memberservice.auth.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
