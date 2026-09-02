package org.example.reservationservice.service;

import org.example.reservationservice.reservation.entity.ReservationStatus;
import org.example.reservationservice.reservation.entity.SessionCapacityLock;
import org.example.reservationservice.reservation.repository.ReservationRepository;
import org.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import org.example.reservationservice.reservation.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HoldConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
    }

    @Test
    void 동시_100건_요청시_확정건수는_정원을_넘지_않는다() throws InterruptedException {
        // given
        UUID sessionId = UUID.randomUUID();
        int threadCount = 100;
        int capacity = 10; // ReservationService.getSessionCapacity()의 고정값과 동일

        // 세션 락 Row를 미리 만들어서 경쟁 상태 제거
//        sessionCapacityLockRepository.save(new SessionCapacityLock(sessionId));

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 100개의 스레드가 동시에 같은 세션에 신청을 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    reservationService.createHoldOrQueue(sessionId, UUID.randomUUID(), 1);
                } catch (Exception e) {
                    System.out.println("실패 발생: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS); // 추가: 완전 종료까지 대기

        Thread.sleep(500); // 추가: DB 커밋 완료를 위한 짧은 대기

        // then: 확정(HOLD) 건수는 정원을 넘지 않아야 함
        List<ReservationStatus> statuses = reservationRepository.findAll().stream()
                .filter(r -> r.getSessionId().equals(sessionId))
                .map(r -> r.getStatus())
                .toList();

        long holdCount = reservationRepository.countBySessionIdAndStatus(sessionId, ReservationStatus.HOLD);
        long queuedCount = reservationRepository.countBySessionIdAndStatus(sessionId, ReservationStatus.QUEUED);

        assertThat(holdCount).isEqualTo(Math.min(threadCount, capacity)); // min(100, 10) = 10
        assertThat(queuedCount).isEqualTo(threadCount - holdCount);       // 나머지는 대기열
        assertThat(holdCount + queuedCount).isEqualTo(threadCount);       // 전체 합 = 100
    }
}