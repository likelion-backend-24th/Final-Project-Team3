package com.example.reservationservice.scheduler;

import com.example.reservationservice.reservation.client.ConferenceServiceClient;
import com.example.reservationservice.reservation.entity.Reservation;
import com.example.reservationservice.reservation.entity.ReservationStatus;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.reservation.repository.SessionCapacityLockRepository;
import com.example.reservationservice.reservation.scheduler.HoldExpirationScheduler;
import com.example.reservationservice.reservation.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest
class HoldExpirationSchedulerTest {

    @Autowired
    private HoldExpirationScheduler scheduler;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SessionCapacityLockRepository sessionCapacityLockRepository;

    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        sessionCapacityLockRepository.deleteAll();
    }

    @Test
    void 만료된_HOLD는_자동으로_취소되고_좌석이_반환된다() {
        // given
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        reservationService.createHoldOrQueue(sessionId, UUID.randomUUID(), 1);

        // 방금 생성된 예약의 expiresAt을 강제로 과거로 조작
        Reservation reservation = reservationRepository.findAll().get(0);
        ReflectionTestUtils.setField(reservation, "expiresAt", LocalDateTime.now().minusMinutes(1));
        reservationRepository.saveAndFlush(reservation);

        // when
        scheduler.expireOverdueHolds();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void 아직_만료되지_않은_HOLD는_취소되지_않는다() {
        UUID sessionId = UUID.randomUUID();
        given(conferenceServiceClient.getSessionCapacity(sessionId)).willReturn(10);

        reservationService.createHoldOrQueue(sessionId, UUID.randomUUID(), 1);
        Reservation reservation = reservationRepository.findAll().get(0);

        scheduler.expireOverdueHolds();

        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.HOLD);
    }
}