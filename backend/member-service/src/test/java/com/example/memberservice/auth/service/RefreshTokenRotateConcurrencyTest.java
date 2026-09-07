package com.example.memberservice.auth.service;

import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.auth.repository.RefreshTokenRepository;
import com.example.memberservice.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RefreshTokenRotateConcurrencyTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
    }

    @Test
    void 동시_재발급_요청시_하나만_성공하고_전체_토큰이_무효화된다() throws InterruptedException {
        // given: 하나의 refresh token을 여러 스레드가 동시에 재발급 시도
        UUID memberId = UUID.randomUUID();
        String rawToken = refreshTokenService.issue(memberId);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger reusedCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    refreshTokenService.rotate(rawToken);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == AuthErrorCode.REFRESH_TOKEN_REUSED) {
                        reusedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // then: 정확히 1개만 성공, 나머지는 전부 재사용으로 거절
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(reusedCount.get()).isEqualTo(threadCount - 1);

        // 재사용 탐지로 새로 발급된 토큰까지 포함해 해당 회원의 모든 토큰이 무효화됨 (기존 순차 재사용 테스트와 동일한 정책)
        boolean anyActive = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getMemberId().equals(memberId))
                .anyMatch(t -> !t.isRevoked());
        assertThat(anyActive).isFalse();
    }
}
