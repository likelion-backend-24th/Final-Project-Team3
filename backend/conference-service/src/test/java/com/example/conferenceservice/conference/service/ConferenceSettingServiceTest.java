package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.dto.ConferenceDescriptionUpdateRequest;
import com.example.conferenceservice.conference.dto.ConferenceLocationUpdateRequest;
import com.example.conferenceservice.conference.dto.ConferenceResponse;
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
class ConferenceSettingServiceTest {

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
    void updateDescription_approvedConference_updatesRegardlessOfStatus() {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conference(organizerId, ConferenceStatus.APPROVED);
        given(conferenceRepository.findById(conference.getId())).willReturn(Optional.of(conference));

        ConferenceResponse result = conferenceService.updateDescription(
                conference.getId(), new ConferenceDescriptionUpdateRequest("수정된 소개글"), organizerId);

        assertThat(result.description()).isEqualTo("수정된 소개글");
        assertThat(result.status()).isEqualTo(ConferenceStatus.APPROVED);
    }

    @Test
    void updateDescription_notOwner_throwsAccessDenied() {
        UUID organizerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Conference conference = conference(organizerId, ConferenceStatus.PENDING);
        given(conferenceRepository.findById(conference.getId())).willReturn(Optional.of(conference));

        assertThatThrownBy(() -> conferenceService.updateDescription(
                conference.getId(), new ConferenceDescriptionUpdateRequest("소개글"), otherId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_ACCESS_DENIED);
    }

    @Test
    void updateLocation_approvedAndAddressUnchanged_updatesOtherFields() {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conference(organizerId, ConferenceStatus.APPROVED);
        given(conferenceRepository.findById(conference.getId())).willReturn(Optional.of(conference));

        ConferenceResponse result = conferenceService.updateLocation(
                conference.getId(),
                new ConferenceLocationUpdateRequest("서울", "버스 이용 권장", "주차 협소", "수유실 운영"),
                organizerId);

        assertThat(result.location()).isEqualTo("서울");
        assertThat(result.transportation()).isEqualTo("버스 이용 권장");
        assertThat(result.status()).isEqualTo(ConferenceStatus.APPROVED);
    }

    @Test
    void updateLocation_approvedAndAddressChanged_throwsLocationLocked() {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conference(organizerId, ConferenceStatus.APPROVED);
        given(conferenceRepository.findById(conference.getId())).willReturn(Optional.of(conference));

        assertThatThrownBy(() -> conferenceService.updateLocation(
                conference.getId(),
                new ConferenceLocationUpdateRequest("부산", null, null, null),
                organizerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_LOCATION_ADDRESS_LOCKED);
        assertThat(conference.getLocation()).isEqualTo("서울");
    }

    @Test
    void updateLocation_pendingAndAddressChanged_updatesLocation() {
        UUID organizerId = UUID.randomUUID();
        Conference conference = conference(organizerId, ConferenceStatus.PENDING);
        given(conferenceRepository.findById(conference.getId())).willReturn(Optional.of(conference));

        ConferenceResponse result = conferenceService.updateLocation(
                conference.getId(),
                new ConferenceLocationUpdateRequest("부산", "지하철 2호선", "지하 주차장", "휠체어 대여"),
                organizerId);

        assertThat(result.location()).isEqualTo("부산");
    }

    @Test
    void updateLocation_notOwner_throwsAccessDenied() {
        UUID organizerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Conference conference = conference(organizerId, ConferenceStatus.PENDING);
        given(conferenceRepository.findById(conference.getId())).willReturn(Optional.of(conference));

        assertThatThrownBy(() -> conferenceService.updateLocation(
                conference.getId(),
                new ConferenceLocationUpdateRequest("부산", null, null, null),
                otherId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_ACCESS_DENIED);
    }

    private Conference conference(UUID organizerId, ConferenceStatus status) {
        return Conference.builder()
                .id(UUID.randomUUID()).organizerId(organizerId).organizerName("주최자").title("컨퍼런스")
                .status(status).capacity(100).location("서울")
                .build();
    }
}
