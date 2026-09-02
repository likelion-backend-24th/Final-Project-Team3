package com.example.conferenceservice.conference.controller;

import com.example.conferenceservice.conference.dto.ConferenceDetailResponseDto;
import com.example.conferenceservice.conference.dto.ConferenceResponseDto;
import com.example.conferenceservice.conference.service.ConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/conferences")
@RequiredArgsConstructor
public class ConferenceController {
    private final ConferenceService conferenceService;

    @GetMapping
    public Page<ConferenceResponseDto> listConferences(@PageableDefault Pageable pageable) {
        return conferenceService.getConferences(pageable).map(ConferenceResponseDto::from);
    }

    @GetMapping("/{id}")
    public ConferenceDetailResponseDto getConference(@PathVariable UUID id) {
        return conferenceService.getConference(id);
    }
}
