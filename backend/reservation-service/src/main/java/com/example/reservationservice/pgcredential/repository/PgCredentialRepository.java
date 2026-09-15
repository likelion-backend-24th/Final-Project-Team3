package com.example.reservationservice.pgcredential.repository;

import com.example.reservationservice.pgcredential.entity.PgCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PgCredentialRepository extends JpaRepository<PgCredential, UUID> {
    Optional<PgCredential> findByProvider(String provider);
}
