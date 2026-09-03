package com.example.conferenceservice.conference.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.dto.ConferenceCapacityResponseDto;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.session.entity.Session;
import com.example.conferenceservice.session.repository.SessionRepository;
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

/**
 * Story 3 계약: Reservation-Service가 예약 전 세션 정원을 조회하는 내부 API.
 * 여기서는 Conference-Service가 정확한 정원을 반환하는지, 그리고
 * 조회 불가 상황(미승인/존재하지 않음)에서 애매한 값 대신 명확한 실패를 반환하는지만 검증한다.
 * Conference-Service 응답 지연·실패 시 Reservation-Service가 정원 초과를 허용하지 않는지는
 * Reservation-Service 쪽 CapacityContractFailureTest의 책임이다.
 */
@ExtendWith(MockitoExtension.class)
class ConferenceCapacityTest {

    @Mock
    private ConferenceRepository conferenceRepository;

    @Mock
    private SessionRepository sessionRepository;

    private ConferenceService conferenceService;

    @BeforeEach
    void setUp() {
        conferenceService = new ConferenceService(conferenceRepository, sessionRepository);
    }

    @Test
    void getCapacity_whenApproved_returnsPerSessionCapacity() {
        UUID conferenceId = UUID.randomUUID();
        Conference approved = Conference.builder()
                .id(conferenceId).organizerId(UUID.randomUUID()).title("승인된 컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
        Session sessionA = Session.builder()
                .id(UUID.randomUUID()).conference(approved).title("세션 A").capacity(30)
                .build();
        Session sessionB = Session.builder()
                .id(UUID.randomUUID()).conference(approved).title("세션 B").capacity(50)
                .build();
        given(conferenceRepository.findByIdAndStatus(conferenceId, ConferenceStatus.APPROVED))
                .willReturn(Optional.of(approved));
        given(sessionRepository.findByConferenceId(conferenceId)).willReturn(List.of(sessionA, sessionB));

        ConferenceCapacityResponseDto result = conferenceService.getCapacity(conferenceId);

        assertThat(result.conferenceId()).isEqualTo(conferenceId);
        assertThat(result.sessions())
                .extracting("id", "capacity")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(sessionA.getId(), 30),
                        org.assertj.core.groups.Tuple.tuple(sessionB.getId(), 50)
                );
    }

    @Test
    void getCapacity_whenNotApprovedOrMissing_failsClosed() {
        UUID missingId = UUID.randomUUID();
        given(conferenceRepository.findByIdAndStatus(missingId, ConferenceStatus.APPROVED))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> conferenceService.getCapacity(missingId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_NOT_FOUND);
    }
}
