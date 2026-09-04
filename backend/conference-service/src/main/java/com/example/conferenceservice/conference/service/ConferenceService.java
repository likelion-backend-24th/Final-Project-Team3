package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.auth.CustomUserDetails;
import com.example.conferenceservice.conference.dto.ConferenceDetailResponse;
import com.example.conferenceservice.conference.dto.ConferenceRequest;
import com.example.conferenceservice.conference.dto.ConferenceResponse;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.entity.ConferenceTag;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.conference.repository.ConferenceTagRepository;
import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.session.entity.Session;
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
    public Page<Conference> getConferences(Pageable pageable) {
        return conferenceRepository.findByStatus(ConferenceStatus.APPROVED, pageable);
    }

    @Transactional(readOnly = true)
    public ConferenceDetailResponse getConference(UUID id) {
        Conference conference = findApprovedConference(id);
        List<Session> sessions = sessionRepository.findByConferenceId(id);
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
}
