package com.example.memberservice.auth.passwordreset.repository;

import com.example.memberservice.auth.passwordreset.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    // 확인 시 조회 - 아직 사용되지 않은 가장 최근 코드
    Optional<PasswordResetToken> findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    // 재발송 쿨다운 체크용
    Optional<PasswordResetToken> findFirstByEmailOrderByCreatedAtDesc(String email);

    // 재발송 전 기존 코드 정리
    @Modifying
    @Query("delete from PasswordResetToken t where t.email = :email")
    void deleteAllByEmail(@Param("email") String email);

    // 원자적 증가
    @Modifying
    @Query("update PasswordResetToken t set t.attempts = t.attempts + 1 where t.id = :id")
    int increaseAttempts(@Param("id") UUID id);
}
