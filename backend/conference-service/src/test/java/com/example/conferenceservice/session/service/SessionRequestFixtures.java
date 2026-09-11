package com.example.conferenceservice.session.service;

import com.example.conferenceservice.session.dto.SessionCreateRequest;
import com.example.conferenceservice.session.dto.SessionUpdateRequest;

import java.time.LocalDateTime;

/**
 * SessionCreateRequest/SessionUpdateRequest의 필수 필드를 모두 채운, 검증을 통과하는 기본값을 제공한다.
 * 개별 테스트가 검증하려는 필드만 다르게 넘기고 나머지는 이 기본값을 재사용한다.
 */
final class SessionRequestFixtures {

    private SessionRequestFixtures() {}

    static SessionCreateRequest validCreateRequest(String title, int capacity) {
        return new SessionCreateRequest(
                title, capacity, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(5).plusHours(1),
                "그랜드홀 A", "김연수 CTO", 10000, 4);
    }

    static SessionUpdateRequest validUpdateRequest(int capacity) {
        return new SessionUpdateRequest(
                capacity, LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(4),
                LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(5).plusHours(1),
                "그랜드홀 A", "김연수 CTO", 10000, 4);
    }
}
