package com.example.conferenceservice.faq.service;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.common.security.OwnerScopeGuard;
import com.example.conferenceservice.conference.entity.Conference;
import com.example.conferenceservice.conference.exception.ConferenceErrorCode;
import com.example.conferenceservice.conference.repository.ConferenceRepository;
import com.example.conferenceservice.faq.dto.FaqRequest;
import com.example.conferenceservice.faq.dto.FaqResponse;
import com.example.conferenceservice.faq.entity.ConferenceFaq;
import com.example.conferenceservice.faq.exception.FaqErrorCode;
import com.example.conferenceservice.faq.repository.ConferenceFaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaqService {
    private final ConferenceFaqRepository faqRepository;
    private final ConferenceRepository conferenceRepository;

    @Transactional(readOnly = true)
    public List<FaqResponse> getFaqs(UUID conferenceId) {
        return faqRepository.findByConferenceIdOrderByCreatedAtDesc(conferenceId).stream()
                .map(FaqResponse::from)
                .toList();
    }

    @Transactional
    public FaqResponse createFaq(UUID conferenceId, FaqRequest request, UUID requesterId) {
        Conference conference = findConference(conferenceId);
        OwnerScopeGuard.verify(requesterId, conference.getOrganizerId(), FaqErrorCode.FAQ_ACCESS_DENIED);

        ConferenceFaq faq = ConferenceFaq.builder()
                .conference(conference)
                .question(request.question())
                .answer(request.answer())
                .build();
        return FaqResponse.from(faqRepository.save(faq));
    }

    @Transactional
    public FaqResponse updateFaq(UUID faqId, FaqRequest request, UUID requesterId) {
        ConferenceFaq faq = findFaq(faqId);
        OwnerScopeGuard.verify(requesterId, faq.getConference().getOrganizerId(), FaqErrorCode.FAQ_ACCESS_DENIED);
        faq.update(request.question(), request.answer());
        return FaqResponse.from(faq);
    }

    @Transactional
    public void deleteFaq(UUID faqId, UUID requesterId) {
        ConferenceFaq faq = findFaq(faqId);
        OwnerScopeGuard.verify(requesterId, faq.getConference().getOrganizerId(), FaqErrorCode.FAQ_ACCESS_DENIED);
        faqRepository.delete(faq);
    }

    private Conference findConference(UUID conferenceId) {
        return conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND));
    }

    private ConferenceFaq findFaq(UUID faqId) {
        return faqRepository.findById(faqId)
                .orElseThrow(() -> new BusinessException(FaqErrorCode.FAQ_NOT_FOUND));
    }
}
