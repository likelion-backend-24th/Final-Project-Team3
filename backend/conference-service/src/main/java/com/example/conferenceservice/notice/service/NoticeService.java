package com.example.conferenceservice.notice.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.common.security.OwnerScopeGuard;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.notice.dto.NoticeRequest;
import com.example.conferenceservice.notice.dto.NoticeResponse;
import com.example.conferenceservice.notice.entity.ConferenceNotice;
import com.example.conferenceservice.notice.exception.NoticeErrorCode;
import com.example.conferenceservice.notice.repository.ConferenceNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final ConferenceNoticeRepository noticeRepository;
    private final ConferenceRepository conferenceRepository;

    @Transactional(readOnly = true)
    public List<NoticeResponse> getNotices(UUID conferenceId) {
        return noticeRepository.findByConferenceIdOrderByCreatedAtDesc(conferenceId).stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @Transactional
    public NoticeResponse createNotice(UUID conferenceId, NoticeRequest request, UUID requesterId) {
        Conference conference = findConference(conferenceId);
        OwnerScopeGuard.verify(requesterId, conference.getOrganizerId(), NoticeErrorCode.NOTICE_ACCESS_DENIED);

        ConferenceNotice notice = ConferenceNotice.builder()
                .conference(conference)
                .title(request.title())
                .content(request.content())
                .build();
        return NoticeResponse.from(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeResponse updateNotice(UUID noticeId, NoticeRequest request, UUID requesterId) {
        ConferenceNotice notice = findNotice(noticeId);
        OwnerScopeGuard.verify(requesterId, notice.getConference().getOrganizerId(), NoticeErrorCode.NOTICE_ACCESS_DENIED);
        notice.update(request.title(), request.content());
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void deleteNotice(UUID noticeId, UUID requesterId) {
        ConferenceNotice notice = findNotice(noticeId);
        OwnerScopeGuard.verify(requesterId, notice.getConference().getOrganizerId(), NoticeErrorCode.NOTICE_ACCESS_DENIED);
        noticeRepository.delete(notice);
    }

    private Conference findConference(UUID conferenceId) {
        return conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
    }

    private ConferenceNotice findNotice(UUID noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }
}
