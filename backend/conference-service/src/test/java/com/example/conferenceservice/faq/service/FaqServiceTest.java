package com.example.conferenceservice.faq.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.entity.ConferenceStatus;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.faq.dto.FaqRequest;
import com.example.conferenceservice.faq.dto.FaqResponse;
import com.example.conferenceservice.faq.entity.ConferenceFaq;
import com.example.conferenceservice.faq.exception.FaqErrorCode;
import com.example.conferenceservice.faq.repository.ConferenceFaqRepository;
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
class FaqServiceTest {

    @Mock
    private ConferenceFaqRepository faqRepository;

    @Mock
    private ConferenceRepository conferenceRepository;

    private FaqService faqService;

    @BeforeEach
    void setUp() {
        faqService = new FaqService(faqRepository, conferenceRepository);
    }

    @Test
    void createFaq_ownerRequest_savesFaq() {
        UUID organizerId = UUID.randomUUID();
        UUID conferenceId = UUID.randomUUID();
        Conference conference = conference(conferenceId, organizerId);
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(conference));
        given(faqRepository.save(org.mockito.ArgumentMatchers.any(ConferenceFaq.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        FaqResponse response = faqService.createFaq(conferenceId, new FaqRequest("취소할 수 있나요?", "3일 전까지 가능합니다."), organizerId);

        assertThat(response.question()).isEqualTo("취소할 수 있나요?");
        verify(faqRepository).save(org.mockito.ArgumentMatchers.any(ConferenceFaq.class));
    }

    @Test
    void createFaq_notOwner_throwsAccessDenied() {
        UUID organizerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID conferenceId = UUID.randomUUID();
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.of(conference(conferenceId, organizerId)));

        assertThatThrownBy(() -> faqService.createFaq(conferenceId, new FaqRequest("q", "a"), otherId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(FaqErrorCode.FAQ_ACCESS_DENIED);
    }

    @Test
    void createFaq_conferenceMissing_throwsNotFound() {
        UUID conferenceId = UUID.randomUUID();
        given(conferenceRepository.findById(conferenceId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> faqService.createFaq(conferenceId, new FaqRequest("q", "a"), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ConferenceErrorCode.CONFERENCE_NOT_FOUND);
    }

    @Test
    void updateFaq_ownerRequest_updatesContent() {
        UUID organizerId = UUID.randomUUID();
        UUID faqId = UUID.randomUUID();
        ConferenceFaq faq = faq(faqId, conference(UUID.randomUUID(), organizerId));
        given(faqRepository.findById(faqId)).willReturn(Optional.of(faq));

        FaqResponse response = faqService.updateFaq(faqId, new FaqRequest("수정된 질문", "수정된 답변"), organizerId);

        assertThat(response.question()).isEqualTo("수정된 질문");
        assertThat(response.answer()).isEqualTo("수정된 답변");
    }

    @Test
    void updateFaq_notOwner_throwsAccessDenied() {
        UUID organizerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID faqId = UUID.randomUUID();
        given(faqRepository.findById(faqId)).willReturn(Optional.of(faq(faqId, conference(UUID.randomUUID(), organizerId))));

        assertThatThrownBy(() -> faqService.updateFaq(faqId, new FaqRequest("q", "a"), otherId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(FaqErrorCode.FAQ_ACCESS_DENIED);
    }

    @Test
    void deleteFaq_ownerRequest_deletesFaq() {
        UUID organizerId = UUID.randomUUID();
        UUID faqId = UUID.randomUUID();
        ConferenceFaq faq = faq(faqId, conference(UUID.randomUUID(), organizerId));
        given(faqRepository.findById(faqId)).willReturn(Optional.of(faq));

        faqService.deleteFaq(faqId, organizerId);

        verify(faqRepository).delete(faq);
    }

    @Test
    void getFaqs_returnsAllForConference() {
        UUID conferenceId = UUID.randomUUID();
        given(faqRepository.findByConferenceIdOrderByCreatedAtDesc(conferenceId))
                .willReturn(List.of(faq(UUID.randomUUID(), conference(conferenceId, UUID.randomUUID()))));

        List<FaqResponse> result = faqService.getFaqs(conferenceId);

        assertThat(result).hasSize(1);
    }

    private Conference conference(UUID id, UUID organizerId) {
        return Conference.builder()
                .id(id).organizerId(organizerId).title("컨퍼런스")
                .status(ConferenceStatus.APPROVED).capacity(100)
                .build();
    }

    private ConferenceFaq faq(UUID id, Conference conference) {
        return ConferenceFaq.builder()
                .id(id).conference(conference).question("원래 질문").answer("원래 답변")
                .build();
    }
}
