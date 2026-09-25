package com.example.memberservice.member.repository;

import com.example.memberservice.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    boolean existsByEmail(String email);
    Optional<Member> findByEmail(String email);
    boolean existsByBusinessNo(String businessNo);

    @Query("SELECT m FROM Member m WHERE " +
            "(:keyword IS NULL OR m.email LIKE %:keyword% OR m.name LIKE %:keyword%)")
    Page<Member> searchMembers(@Param("keyword") String keyword, Pageable pageable);
}
