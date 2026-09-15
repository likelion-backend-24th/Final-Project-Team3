package com.example.conferenceservice.attendeesummary.service;

import com.example.conferenceservice.attendeesummary.client.AttendeeCheckinStatsClient;
import com.example.conferenceservice.attendeesummary.client.AttendeeCheckinStatsClient.AttendeeCheckinStatsResponse;
import com.example.conferenceservice.attendeesummary.client.AttendeeStatsUnavailableException;
import com.example.conferenceservice.attendeesummary.dto.ConferenceAttendeeSummaryResponse;
import com.example.conferenceservice.attendeesummary.entity.ConferenceAttendeeSummary;
import com.example.conferenceservice.attendeesummary.exception.AttendeeSummaryErrorCode;
import com.example.conferenceservice.attendeesummary.repository.ConferenceAttendeeSummaryRepository;
import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.common.security.OwnerScopeGuard;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 16-2의 ConferenceOperationStatusService와 같은 이유로 @Transactional을 의도적으로 붙이지 않는다:
 * Reservation-Service 호출이 실패해 지연되면 DB 커넥션 풀을 오래 점유하게 되기 때문.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConferenceAttendeeSummaryService {

    private static final String ZERO_CHECKIN_MESSAGE = "아직 체크인한 참석자가 없습니다.";

    private final ConferenceRepository conferenceRepository;
    private final SessionRepository sessionRepository;
    private final ConferenceAttendeeSummaryRepository summaryRepository;
    private final AttendeeCheckinStatsClient attendeeCheckinStatsClient;
    private final ObjectMapper objectMapper;

    public ConferenceAttendeeSummaryResponse getAttendeeSummary(UUID conferenceId, UUID requesterId) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
        OwnerScopeGuard.verify(requesterId, conference.getOrganizerId(), ConferenceErrorCode.CONFERENCE_ACCESS_DENIED);

        List<UUID> sessionIds = sessionRepository.findByConferenceId(conferenceId).stream()
                .map(Session::getId)
                .toList();

        AttendeeCheckinStatsResponse stats = fetchStats(sessionIds);
        Optional<ConferenceAttendeeSummary> cached = summaryRepository.findByConferenceId(conferenceId);

        if (cached.isPresent() && cached.get().getCheckedInCount() == stats.checkedInCount()) {
            return toResponse(cached.get());
        }
        return toResponse(recalculate(conferenceId, stats, cached));
    }

    private AttendeeCheckinStatsResponse fetchStats(List<UUID> sessionIds) {
        try {
            return attendeeCheckinStatsClient.getAttendeeCheckinStats(sessionIds);
        } catch (AttendeeStatsUnavailableException e) {
            log.warn("Reservation-Service 응답 실패로 참석자 통계 조회를 중단합니다: sessionIds={}", sessionIds, e);
            throw new BusinessException(AttendeeSummaryErrorCode.RESERVATION_SERVICE_UNAVAILABLE);
        }
    }

    private ConferenceAttendeeSummary recalculate(UUID conferenceId, AttendeeCheckinStatsResponse stats,
                                                  Optional<ConferenceAttendeeSummary> cached) {
        String ageJson = objectMapper.writeValueAsString(stats.ageGroupDistribution());
        String jobJson = objectMapper.writeValueAsString(stats.jobDistribution());
        String summaryText = stats.checkedInCount() == 0
                ? ZERO_CHECKIN_MESSAGE
                : null; // TODO(Task 15-3): LLM 요약 생성으로 교체

        ConferenceAttendeeSummary entity = cached
                .map(existing -> {
                    existing.update(stats.checkedInCount(), ageJson, jobJson, summaryText);
                    return existing;
                })
                .orElseGet(() -> ConferenceAttendeeSummary.builder()
                        .conferenceId(conferenceId)
                        .checkedInCount(stats.checkedInCount())
                        .ageGroupDistributionJson(ageJson)
                        .jobDistributionJson(jobJson)
                        .summaryText(summaryText)
                        .generatedAt(Instant.now())
                        .build());

        return summaryRepository.save(entity);
    }

    private ConferenceAttendeeSummaryResponse toResponse(ConferenceAttendeeSummary entity) {
        return new ConferenceAttendeeSummaryResponse(
                entity.getConferenceId(),
                entity.getCheckedInCount(),
                objectMapper.readValue(entity.getAgeGroupDistributionJson(), new TypeReference<Map<String, Long>>() {}),
                objectMapper.readValue(entity.getJobDistributionJson(), new TypeReference<Map<String, Long>>() {}),
                entity.getSummaryText(),
                entity.getGeneratedAt());
    }
}