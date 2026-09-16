package com.example.conferenceservice.organizerprofile.service;

import com.example.conferenceservice.attendeesummary.entity.ConferenceAttendeeSummary;
import com.example.conferenceservice.attendeesummary.repository.ConferenceAttendeeSummaryRepository;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.organizerprofile.dto.OrganizerProfileResponse;
import com.example.conferenceservice.organizerprofile.dto.PastConferenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        List<PastConferenceResponse> pastConferences = approvedConferences.stream()
                .map(this::toPastConferenceResponse)
                .toList();

        return new OrganizerProfileResponse(organizerId, organizerName, pastConferences);
    }

    private PastConferenceResponse toPastConferenceResponse(Conference conference) {
        // 아직 Story 15 요약이 생성 안 됐거나 체크인이 하나도 없었으면 summary가 없을 수 있음 — null로 반환(에러 아님)
        String summaryText = summaryRepository.findByConferenceId(conference.getId())
                .map(ConferenceAttendeeSummary::getSummaryText)
                .orElse(null);

        return new PastConferenceResponse(
                conference.getId(),
                conference.getTitle(),
                conference.getStartAt(),
                conference.getEndAt(),
                conference.getLocation(),
                summaryText);
    }

    public record OrganizerSummary(int pastConferenceCount, String representativeSummary) {}

    public OrganizerSummary getOrganizerSummary(UUID organizerId, UUID excludeConferenceId) {
        List<PastConferenceResponse> pastConferences = getOrganizerProfile(organizerId).pastConferences().stream()
                .filter(pc -> !pc.conferenceId().equals(excludeConferenceId))
                .toList();

        String representativeSummary = pastConferences.stream()
                .filter(pc -> pc.summaryText() != null)
                .max(Comparator.comparing(PastConferenceResponse::endAt))
                .map(PastConferenceResponse::summaryText)
                .orElse(null);

        return new OrganizerSummary(pastConferences.size(), representativeSummary);
    }
}
