package com.example.conferenceservice.notice.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.notice.dto.NoticeRequest;
import com.example.conferenceservice.notice.dto.NoticeResponse;
import com.example.conferenceservice.notice.entity.ConferenceNotice;
import com.example.conferenceservice.notice.exception.NoticeErrorCode;
import com.example.conferenceservice.notice.repository.ConferenceNoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private ConferenceNoticeRepository noticeRepository;

    @Mock
    private ConferenceRepository conferenceRepository;

    private NoticeService noticeService;

    @BeforeEach
    void setUp() {
        noticeService = new NoticeService(noticeRepository, conferenceRepository);
    }

    @Test
    void createNotice_ownerRequest_savesNotice() {
        UUID organizerId = UUID.randomUUID();
        UUID conferenceId = UUID.randomUUID();
        Conference conference = conference(conferenceId, organizerId);
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(conference));
        given(noticeRepository.save(org.mockito.ArgumentMatchers.any(ConferenceNotice.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        NoticeResponse response = noticeService.createNotice(conferenceId, new NoticeRequest("사전 등록 안내", "3일 전까지 가능합니다."), organizerId);

        assertThat(response.title()).isEqualTo("사전 등록 안내");
        verify(noticeRepository).save(org.mockito.ArgumentMatchers.any(ConferenceNotice.class));
    }

    @Test
    void createNotice_notOwner_throwsAccessDenied() {
        UUID organizerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID conferenceId = UUID.randomUUID();
        Conference conference = conference(conferenceId, organizerId);
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(conference));

        assertThatThrownBy(() -> noticeService.createNotice(conferenceId, new NoticeRequest("t", "c"), otherId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(NoticeErrorCode.NOTICE_ACCESS_DENIED);
    }

    @Test
    void createNotice_conferenceMissing_throwsNotFound() {
        UUID conferenceId = UUID.randomUUID();
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.createNotice(conferenceId, new NoticeRequest("t", "c"), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_NOT_FOUND);
    }

    @Test
    void updateNotice_ownerRequest_updatesContent() {
        UUID organizerId = UUID.randomUUID();
        UUID noticeId = UUID.randomUUID();
        ConferenceNotice notice = notice(noticeId, conference(UUID.randomUUID(), organizerId));
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        NoticeResponse response = noticeService.updateNotice(noticeId, new NoticeRequest("수정된 제목", "수정된 내용"), organizerId);

        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.content()).isEqualTo("수정된 내용");
    }

    @Test
    void updateNotice_notOwner_throwsAccessDenied() {
        UUID organizerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID noticeId = UUID.randomUUID();
        ConferenceNotice notice = notice(noticeId, conference(UUID.randomUUID(), organizerId));
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        assertThatThrownBy(() -> noticeService.updateNotice(noticeId, new NoticeRequest("t", "c"), otherId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(NoticeErrorCode.NOTICE_ACCESS_DENIED);
    }

    @Test
    void deleteNotice_ownerRequest_deletesNotice() {
        UUID organizerId = UUID.randomUUID();
        UUID noticeId = UUID.randomUUID();
        ConferenceNotice notice = notice(noticeId, conference(UUID.randomUUID(), organizerId));
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        noticeService.deleteNotice(noticeId, organizerId);

        verify(noticeRepository).delete(notice);
    }

    @Test
    void getNotices_returnsAllForConference() {
        UUID conferenceId = UUID.randomUUID();
        ConferenceNotice notice = notice(UUID.randomUUID(), conference(conferenceId, UUID.randomUUID()));
        given(noticeRepository.findByConferenceIdOrderByCreatedAtDesc(conferenceId)).willReturn(List.of(notice));

        List<NoticeResponse> result = noticeService.getNotices(conferenceId);

        assertThat(result).hasSize(1);
    }

    private Conference conference(UUID id, UUID organizerId) {
        return Conference.builder()
                .id(id).organizerId(organizerId).title("컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
    }

    private ConferenceNotice notice(UUID id, Conference conference) {
        return ConferenceNotice.builder()
                .id(id).conference(conference).title("원래 제목").content("원래 내용")
                .build();
    }
}
