package com.example.conferenceservice.organizerprofile.controller;

import com.example.conferenceservice.common.TraceIdProvider;
import com.example.conferenceservice.common.dto.ApiResponse;
import com.example.conferenceservice.organizerprofile.dto.OrganizerProfileResponse;
import com.example.conferenceservice.organizerprofile.service.OrganizerProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/organizers")
@RequiredArgsConstructor
public class OrganizerProfileController {

    private final OrganizerProfileService organizerProfileService;
    private final TraceIdProvider traceIdProvider;

    @GetMapping("/{organizerId}/profile")
    public ResponseEntity<ApiResponse<OrganizerProfileResponse>> getOrganizerProfile(
            @PathVariable UUID organizerId,
            HttpServletRequest request
            ){
        OrganizerProfileResponse response = organizerProfileService.getOrganizerProfile(organizerId);
        return ResponseEntity.ok(ApiResponse.success("주최자 프로필 조회 성공", response, traceIdProvider.resolve(request)));
    }
}
