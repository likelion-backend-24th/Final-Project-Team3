package com.example.memberservice.auth.repository;

import com.example.memberservice.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    // 검증 시 조회 - 아직 verified=false인 가장 최근 코드
    Optional<EmailVerification> findFirstByEmailAndVerifiedFalseOrderByCreatedAtDesc(String email);
    // 회원가입 시 게이트 체크 - 이 이메일이 인증 완료 상태인지
    boolean existsByEmailAndVerifiedTrue(String email);
    // 재발송 전 기존 코드 정리 + 가입 성공 후 소진 처리에 재사용
    @Modifying
    @Query("delete from EmailVerification e where e.email = :email")
    void deleteAllByEmail(@Param("email") String email);

    // 읽기->고치기->쓰기 대신 DB에서 바로 증가 (원자적 UPDATE)
    @Modifying
    @Query("update EmailVerification e set e.attempts = e.attempts + 1 where e.id = :id")
    int increaseAttempts(@Param("id") UUID id);
}
