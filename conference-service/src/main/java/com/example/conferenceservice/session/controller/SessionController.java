package com.example.conferenceservice.session.controller;

import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.session.dto.SessionCapacityResponseDto;
import com.example.conferenceservice.session.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping("/{sessionId}/capacity")
    public ApiResponse<SessionCapacityResponseDto> getCapacity(@PathVariable UUID sessionId, HttpServletRequest request) {
        SessionCapacityResponseDto capacity = sessionService.getCapacity(sessionId);
        return ApiResponse.success("세션 정원 조회 성공", capacity, traceIdProvider.resolve(request));
    }
}
