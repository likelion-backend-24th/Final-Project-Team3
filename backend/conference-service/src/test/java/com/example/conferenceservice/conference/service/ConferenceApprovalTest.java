package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.dto.ConferenceResponse;
import com.example.conferenceservice.conference.dto.RejectConferenceRequest;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.conference.repository.ConferenceTagRepository;
import com.example.conferenceservice.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ConferenceApprovalTest {

    @Mock
    private ConferenceRepository conferenceRepository;

    @Mock
    private ConferenceTagRepository conferenceTagRepository;

    @Mock
    private SessionRepository sessionRepository;

    private ConferenceService conferenceService;

    @BeforeEach
    void setUp() {
        conferenceService = new ConferenceService(conferenceRepository, conferenceTagRepository, sessionRepository);
    }

    @Test
    void approveConference_whenPending_setsStatusApproved() {
        UUID conferenceId = UUID.randomUUID();
        Conference pending = Conference.builder()
                .id(conferenceId).organizerId(UUID.randomUUID()).organizerName("주최자").title("신청된 컨퍼런스")
                .status(ConferenceStatus.PENDING).capacity(100)
                .build();
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(pending));

        ConferenceResponse result = conferenceService.approveConference(conferenceId);

        assertThat(result.status()).isEqualTo(ConferenceStatus.APPROVED);
    }

    @Test
    void approveConference_whenAlreadyDecided_throwsBusinessException() {
        UUID conferenceId = UUID.randomUUID();
        Conference approved = Conference.builder()
                .id(conferenceId).organizerId(UUID.randomUUID()).organizerName("주최자").title("이미 승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(approved));

        assertThatThrownBy(() -> conferenceService.approveConference(conferenceId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_ALREADY_DECIDED);
    }

    @Test
    void approveConference_whenNotFound_throwsBusinessException() {
        UUID missingId = UUID.randomUUID();
        given(conferenceRepository.findById(missingId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> conferenceService.approveConference(missingId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_NOT_FOUND);
    }

    @Test
    void rejectConference_whenPending_setsStatusRejectedWithReason() {
        UUID conferenceId = UUID.randomUUID();
        Conference pending = Conference.builder()
                .id(conferenceId).organizerId(UUID.randomUUID()).organizerName("주최자").title("신청된 컨퍼런스")
                .status(ConferenceStatus.PENDING).capacity(100)
                .build();
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(pending));

        ConferenceResponse result = conferenceService.rejectConference(conferenceId, new RejectConferenceRequest("정원 초과"));

        assertThat(result.status()).isEqualTo(ConferenceStatus.REJECTED);
        assertThat(pending.getRejectionReason()).isEqualTo("정원 초과");
    }

    @Test
    void rejectConference_whenAlreadyDecided_throwsBusinessException() {
        UUID conferenceId = UUID.randomUUID();
        Conference rejected = Conference.builder()
                .id(conferenceId).organizerId(UUID.randomUUID()).organizerName("주최자").title("이미 반려된 컨퍼런스")
                .status(ConferenceStatus.REJECTED).capacity(100)
                .build();
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(rejected));

        assertThatThrownBy(() -> conferenceService.rejectConference(conferenceId, new RejectConferenceRequest("사유")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_ALREADY_DECIDED);
    }
}
