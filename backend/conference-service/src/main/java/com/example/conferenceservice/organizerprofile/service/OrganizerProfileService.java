package com.example.conferenceservice.organizerprofile.service;

import com.example.conferenceservice.attendeesummary.entity.ConferenceAttendeeSummary;
import com.example.conferenceservice.attendeesummary.repository.ConferenceAttendeeSummaryRepository;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.organizerprofile.dto.OrganizerConferenceResponse;
import com.example.conferenceservice.organizerprofile.dto.OrganizerProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizerProfileService {

    private final ConferenceRepository conferenceRepository;
    private final ConferenceAttendeeSummaryRepository summaryRepository;

    public OrganizerProfileResponse getOrganizerProfile(UUID organizerId) {
        List<Conference> approvedConferences =
                conferenceRepository.findByOrganizerIdAndStatus(organizerId, ConferenceStatus.APPROVED);

        String organizerName = approvedConferences.stream()
                .findFirst()
                .map(Conference::getOrganizerName)
                .orElse(null);

        // 승인(APPROVED) 상태만으로는 "지난" 컨퍼런스인지 알 수 없다 — 종료 시각(endAt) 기준으로
        // 이미 끝난 것(지난 컨퍼런스)과 아직 안 끝난 것(진행중·예정 컨퍼런스)을 나눠서 보여준다.
        // endAt이 없는 경우(데이터 누락)는 아직 안 끝난 쪽으로 취급한다.
        LocalDateTime now = LocalDateTime.now();
        List<OrganizerConferenceResponse> pastConferences = approvedConferences.stream()
                .filter(c -> c.getEndAt() != null && c.getEndAt().isBefore(now))
                .map(this::toOrganizerConferenceResponse)
                .toList();
        List<OrganizerConferenceResponse> ongoingConferences = approvedConferences.stream()
                .filter(c -> c.getEndAt() == null || !c.getEndAt().isBefore(now))
                .map(this::toOrganizerConferenceResponse)
                .toList();

        return new OrganizerProfileResponse(organizerId, organizerName, pastConferences, ongoingConferences);
    }

    private OrganizerConferenceResponse toOrganizerConferenceResponse(Conference conference) {
        // 아직 Story 15 요약이 생성 안 됐거나 체크인이 하나도 없었으면 summary가 없을 수 있음 — null로 반환(에러 아님)
        String summaryText = summaryRepository.findByConferenceId(conference.getId())
                .map(ConferenceAttendeeSummary::getSummaryText)
                .orElse(null);

        return new OrganizerConferenceResponse(
                conference.getId(),
                conference.getTitle(),
                conference.getStartAt(),
                conference.getEndAt(),
                conference.getLocation(),
                summaryText);
    }

    public record OrganizerSummary(int pastConferenceCount, String representativeSummary) {}

    public OrganizerSummary getOrganizerSummary(UUID organizerId, UUID excludeConferenceId) {
        List<OrganizerConferenceResponse> pastConferences = getOrganizerProfile(organizerId).pastConferences().stream()
                .filter(pc -> !pc.conferenceId().equals(excludeConferenceId))
                .toList();

        String representativeSummary = pastConferences.stream()
                .filter(pc -> pc.summaryText() != null)
                .max(Comparator.comparing(OrganizerConferenceResponse::endAt))
                .map(OrganizerConferenceResponse::summaryText)
                .orElse(null);

        return new OrganizerSummary(pastConferences.size(), representativeSummary);
    }
}
