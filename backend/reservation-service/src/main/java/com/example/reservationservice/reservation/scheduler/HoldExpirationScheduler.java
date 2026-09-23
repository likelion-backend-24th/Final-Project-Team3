package com.example.reservationservice.reservation.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpirationScheduler {

    private final QueuePromotionService queuePromotionService;

    @Scheduled(fixedRate = 60000)
    public void expireOverdueHolds() {
        List<UUID> affectedSessionIds = queuePromotionService.expireHolds();

        for (UUID sessionId : affectedSessionIds) {
            queuePromotionService.promoteQueueIfCapacityAvailable(sessionId);
        }
    }
}