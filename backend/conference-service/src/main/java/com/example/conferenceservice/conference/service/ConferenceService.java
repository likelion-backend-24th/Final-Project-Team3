package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.auth.CustomUserDetails;
import com.example.conferenceservice.auth.MemberRole;
import com.example.conferenceservice.common.file.FileStorageService;
import com.example.conferenceservice.conference.dto.ConferenceDescriptionUpdateRequest;
import com.example.conferenceservice.conference.dto.ConferenceDetailResponse;
import com.example.conferenceservice.conference.dto.ConferenceLocationUpdateRequest;
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
import com.example.conferenceservice.common.security.OwnerScopeGuard;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.entity.SessionStatus;
import com.example.conferenceservice.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConferenceService {
    private final ConferenceRepository conferenceRepository;
    private final ConferenceTagRepository conferenceTagRepository;
    private final SessionRepository sessionRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ConferenceResponse applyConference(CustomUserDetails currentUser, ConferenceRequest request, MultipartFile proofFile) {
        if (currentUser.getOrganizationName() == null || currentUser.getOrganizationName().isBlank()) {
            throw new BusinessException(ConferenceErrorCode.ORGANIZATION_NAME_NOT_FOUND);
        }
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException(ConferenceErrorCode.INVALID_CONFERENCE_PERIOD);
        }

        String proofFileName = (proofFile != null && !proofFile.isEmpty())
                ? fileStorageService.store(proofFile)
                : null;

        Conference conference = Conference.builder()
                .organizerId(currentUser.getMemberId())
                .organizerName(currentUser.getOrganizationName())
                .title(request.title())
                .capacity(request.capacity())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .location(request.location())
                .transportation(request.transportation())
                .parkingInfo(request.parkingInfo())
                .amenities(request.amenities())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .proofFileName(proofFileName)
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
    public Page<ConferenceResponse> getMyConferences(UUID organizerId, Pageable pageable) {
        Page<Conference> conferences = conferenceRepository.findByOrganizerId(organizerId, pageable);
        return toResponsePage(conferences, countSessionsByConference(conferences.getContent()));
    }

    @Transactional(readOnly = true)
    public Page<ConferenceResponse> getConferences(Pageable pageable) {
        Page<Conference> conferences = conferenceRepository.findByStatus(ConferenceStatus.APPROVED, pageable);
        return toResponsePage(conferences, countApprovedSessionsByConference(conferences.getContent()));
    }

    private Page<ConferenceResponse> toResponsePage(Page<Conference> conferences, Map<UUID, Long> sessionCounts) {
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

    private Map<UUID, Long> countSessionsByConference(List<Conference> conferences) {
        if (conferences.isEmpty()) {
            return Map.of();
        }
        List<UUID> conferenceIds = conferences.stream().map(Conference::getId).toList();
        return sessionRepository.countByConferenceIdIn(conferenceIds).stream()
                .collect(Collectors.toMap(
                        SessionRepository.ConferenceSessionCount::getConferenceId,
                        SessionRepository.ConferenceSessionCount::getCount));
    }

    private long countApprovedSessions(Conference conference) {
        return countApprovedSessionsByConference(List.of(conference)).getOrDefault(conference.getId(), 0L);
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

    @Transactional
    public ConferenceResponse updateDescription(UUID id, ConferenceDescriptionUpdateRequest request, UUID requesterId) {
        Conference conference = findConference(id);
        OwnerScopeGuard.verify(requesterId, conference.getOrganizerId(), ConferenceErrorCode.CONFERENCE_ACCESS_DENIED);
        conference.updateDescription(request.description());
        return ConferenceResponse.from(conference, countApprovedSessions(conference));
    }

    @Transactional
    public ConferenceResponse updateLocation(UUID id, ConferenceLocationUpdateRequest request, UUID requesterId) {
        Conference conference = findConference(id);
        OwnerScopeGuard.verify(requesterId, conference.getOrganizerId(), ConferenceErrorCode.CONFERENCE_ACCESS_DENIED);
        if (conference.isApproved() && !Objects.equals(conference.getLocation(), request.location())) {
            throw new BusinessException(ConferenceErrorCode.CONFERENCE_LOCATION_ADDRESS_LOCKED);
        }
        conference.updateLocation(request.location(), request.transportation(), request.parkingInfo(), request.amenities());
        return ConferenceResponse.from(conference, countApprovedSessions(conference));
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

    // 승인 전(PENDING)·반려(REJECTED) 상태도 볼 수 있어야 해서 getConference와 달리 상태 제한이 없고, 세션도 승인 여부와 무관하게 전부 보여준다
    @Transactional(readOnly = true)
    public ConferenceDetailResponse getConferenceDetailForAdmin(UUID id) {
        Conference conference = findConference(id);
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

    private Conference findConference(UUID id) {
        return conferenceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
    }

    // 증명 파일은 본인(주최자) 또는 관리자만 열람 가능 - 승인 심사 목적이지 공개 자료가 아니다.
    @Transactional(readOnly = true)
    public Conference getConferenceForProofFileAccess(UUID id, CustomUserDetails currentUser) {
        Conference conference = findConference(id);
        if (currentUser.getRole() != MemberRole.ADMIN) {
            OwnerScopeGuard.verify(currentUser.getMemberId(), conference.getOrganizerId(), ConferenceErrorCode.CONFERENCE_ACCESS_DENIED);
        }
        if (!conference.hasProofFile()) {
            throw new BusinessException(ConferenceErrorCode.PROOF_FILE_NOT_FOUND);
        }
        return conference;
    }
}
