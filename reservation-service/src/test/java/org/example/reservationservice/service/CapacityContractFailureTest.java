package org.example.reservationservice.service;

import org.example.reservationservice.common.exception.BusinessException;
import org.example.reservationservice.reservation.client.ConferenceServiceClient;
import org.example.reservationservice.reservation.exception.ConferenceServiceUnavailableException;
import org.example.reservationservice.reservation.exception.ReservationErrorCode;
import org.example.reservationservice.reservation.repository.ReservationRepository;
import org.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import org.example.reservationservice.reservation.repository.WaitingQueueRepository;
import org.example.reservationservice.reservation.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * Story 3 계약: Conference-Service 응답 지연/실패 시에도 정원 초과가 발생하지 않는다.
 * getSessionCapacity 호출이 실패하면 성공으로 간주하지 않고 예약/대기열 등록 전체를
 * 명확한 실패(CONFERENCE_SERVICE_UNAVAILABLE)로 되돌린다 (fail-closed).
 */
@SpringBootTest
class CapacityContractFailureTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
    }

    @Test
    void conferenceService_응답_실패시_예약을_생성하지_않고_명확히_실패한다() {
        // given: Conference-Service가 Timeout 등으로 응답 불가
        UUID sessionId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId))
                .willThrow(new ConferenceServiceUnavailableException(sessionId, new RuntimeException("timeout")));

        // when & then: 정원 확인 불가를 성공으로 간주하지 않고 명확한 예외로 실패한다
        assertThatThrownBy(() -> reservationService.createHoldOrQueue(sessionId, memberId, 1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ReservationErrorCode.CONFERENCE_SERVICE_UNAVAILABLE);

        // 정원 초과를 허용하지 않음: 예약도, 정원 락도, 대기열도 생성되지 않아야 한다
        assertThat(reservationRepository.count()).isZero();
        assertThat(sessionCapacityLockRepository.findById(sessionId)).isEmpty();
        assertThat(waitingQueueRepository.findMaxPositionBySessionId(sessionId)).isZero();
    }

    @Test
    void conferenceService_응답_실패가_지속되어도_동시_요청_전체가_정원_초과없이_실패한다() throws InterruptedException {
        // given: Conference-Service가 계속 응답 불가 상태
        UUID sessionId = UUID.randomUUID();
        int threadCount = 50;
        given(conferenceServiceClient.getSessionCapacity(sessionId))
                .willThrow(new ConferenceServiceUnavailableException(sessionId, new RuntimeException("timeout")));

        ExecutorService executorService = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> failures = Collections.synchronizedList(new ArrayList<>());

        // when: 여러 스레드가 동시에 같은 세션에 신청을 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    reservationService.createHoldOrQueue(sessionId, UUID.randomUUID(), 1);
                } catch (Exception e) {
                    failures.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // then: 전부 명확하게 실패해야 하고, 확정/대기 예약이 하나도 생기지 않아야 한다 (정원 초과 없음)
        assertThat(failures).hasSize(threadCount);
        assertThat(failures).allMatch(e -> e instanceof BusinessException businessException
                && businessException.getErrorCode() == ReservationErrorCode.CONFERENCE_SERVICE_UNAVAILABLE);
        assertThat(reservationRepository.count()).isZero();
        assertThat(sessionCapacityLockRepository.findById(sessionId)).isEmpty();
    }
}
