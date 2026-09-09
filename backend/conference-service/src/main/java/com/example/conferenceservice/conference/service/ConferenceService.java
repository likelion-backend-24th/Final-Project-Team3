package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.auth.CustomUserDetails;
import com.example.conferenceservice.conference.dto.ConferenceDetailResponse;
import com.example.conferenceservice.conference.dto.ConferenceRequest;
import com.example.conferenceservice.conference.dto.ConferenceResponse;
import com.example.conferenceservice.conference.dto.RejectConferenceRequest;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.entity.ConferenceTag;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.conference.repository.ConferenceTagRepository;
import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
import com.example.conferenceservice.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConferenceService {
    private final ConferenceRepository conferenceRepository;
    private final ConferenceTagRepository conferenceTagRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public ConferenceResponse applyConference(CustomUserDetails currentUser, ConferenceRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException(ConferenceErrorCode.INVALID_CONFERENCE_PERIOD);
        }

        Conference conference = Conference.builder()
                .organizerId(currentUser.getMemberId())
                .organizerName(request.organizerName())
                .title(request.title())
                .capacity(request.capacity())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .location(request.location())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .status(ConferenceStatus.PENDING)
                .build();
        Conference savedConference = conferenceRepository.save(conference);

        List<ConferenceTag> tags = toTags(request.tags(), savedConference);
        if (!tags.isEmpty()) {
            conferenceTagRepository.saveAll(tags);
        }

        return ConferenceResponse.from(savedConference);
    }

    @Transactional(readOnly = true)
    public Page<ConferenceResponse> getConferences(Pageable pageable) {
        Page<Conference> conferences = conferenceRepository.findByStatus(ConferenceStatus.APPROVED, pageable);
        Map<UUID, Long> sessionCounts = countApprovedSessionsByConference(conferences.getContent());
        return conferences.map(conference ->
                ConferenceResponse.from(conference, sessionCounts.getOrDefault(conference.getId(), 0L)));
    }

    private Map<UUID, Long> countApprovedSessionsByConference(List<Conference> conferences) {
        if (conferences.isEmpty()) {
            return Map.of();
        }
        List<UUID> conferenceIds = conferences.stream().map(Conference::getId).toList();
        return sessionRepository.countByConferenceIdInAndStatus(conferenceIds, SessionStatus.APPROVED).stream()
                .collect(Collectors.toMap(
                        SessionRepository.ConferenceSessionCount::getConferenceId,
                        SessionRepository.ConferenceSessionCount::getCount));
    }

    @Transactional(readOnly = true)
    public Page<Conference> getPendingConferences(Pageable pageable) {
        return conferenceRepository.findByStatus(ConferenceStatus.PENDING, pageable);
    }

    @Transactional
    public ConferenceResponse approveConference(UUID id) {
        Conference conference = findConference(id);
        if (!conference.isPending()) {
            throw new BusinessException(ConferenceErrorCode.CONFERENCE_ALREADY_DECIDED);
        }
        conference.approve();
        return ConferenceResponse.from(conference);
    }

    @Transactional
    public ConferenceResponse rejectConference(UUID id, RejectConferenceRequest request) {
        Conference conference = findConference(id);
        if (!conference.isPending()) {
            throw new BusinessException(ConferenceErrorCode.CONFERENCE_ALREADY_DECIDED);
        }
        conference.reject(request.reason());
        return ConferenceResponse.from(conference);
    }

    @Transactional(readOnly = true)
    public ConferenceDetailResponse getConference(UUID id) {
        Conference conference = findApprovedConference(id);
        List<Session> sessions = sessionRepository.findByConferenceIdAndStatus(id, SessionStatus.APPROVED);
        List<String> tags = conferenceTagRepository.findByConferenceId(id).stream()
                .map(ConferenceTag::getTag)
                .toList();
        return ConferenceDetailResponse.from(conference, sessions, tags);
    }

    private List<ConferenceTag> toTags(List<String> tagNames, Conference conference) {
        if (tagNames == null) {
            return List.of();
        }
        return dedupeIgnoringCase(tagNames).stream()
                .map(tag -> ConferenceTag.builder().conference(conference).tag(tag).build())
                .toList();
    }

    private List<String> dedupeIgnoringCase(List<String> tagNames) {
        Map<String, String> deduped = new LinkedHashMap<>();
        for (String tagName : tagNames) {
            String trimmed = tagName.trim();
            deduped.putIfAbsent(trimmed.toLowerCase(), trimmed);
        }
        return List.copyOf(deduped.values());
    }

    private Conference findApprovedConference(UUID id) {
        return conferenceRepository.findByIdAndStatus(id, ConferenceStatus.APPROVED)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
    }

    private Conference findConference(UUID id) {
        return conferenceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
    }
}
