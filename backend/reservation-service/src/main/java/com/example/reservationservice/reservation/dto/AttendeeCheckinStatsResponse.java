package com.example.reservationservice.reservation.dto;

import com.example.reservationservice.reservation.entity.AgeGroup;
import com.example.reservationservice.reservation.entity.Job;

import java.util.Map;

/**
 * 세션별 체크인 완료 참가자의 연령대·직무 분포 집계 응답.
 * Conference-Service가 /internal/sessions/attendee-checkin-stats 호출 시 반환됨.
 *
 * @param checkedInCount 체크인(QR 사용) 완료된 좌석 총 수
 * @param ageGroupDistribution 연령대별 체크인 인원 분포 (연령대 -> 인원 수)
 * @param jobDistribution 직무별 체크인 인원 분포 (직무 -> 인원 수)
 */
public record AttendeeCheckinStatsResponse(
        int checkedInCount,
        Map<AgeGroup, Long> ageGroupDistribution,
        Map<Job, Long> jobDistribution
) {}