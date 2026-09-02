package com.example.conferenceservice.conference.controller;

import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.common.dto.Meta;
import com.example.conferenceservice.common.dto.PageMeta;
import com.example.conferenceservice.conference.dto.ConferenceDetailResponseDto;
import com.example.conferenceservice.conference.dto.ConferenceResponseDto;
import com.example.conferenceservice.conference.service.ConferenceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conferences")
@RequiredArgsConstructor
public class ConferenceController {
    private final ConferenceService conferenceService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping
    public ApiResponse<List<ConferenceResponseDto>> listConferences(@PageableDefault Pageable pageable, HttpServletRequest request) {
        Page<ConferenceResponseDto> page = conferenceService.getConferences(pageable).map(ConferenceResponseDto::from);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ApiResponse.success("컨퍼런스 목록 조회 성공", page.getContent(), meta, traceIdProvider.resolve(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConferenceDetailResponseDto> getConference(@PathVariable UUID id, HttpServletRequest request) {
        ConferenceDetailResponseDto conference = conferenceService.getConference(id);
        return ApiResponse.success("컨퍼런스 상세 조회 성공", conference, traceIdProvider.resolve(request));
    }
}
