package com.example.conferenceservice.organizerprofile.dto;

import java.util.UUID;

// Member-Service가 주최자 탈퇴 처리 전에 호출하는 내부 API 응답.
// "탈퇴해도 되는지" 판단(정책)은 호출하는 쪽(Member-Service)이 하고, 여긴 사실만 알려준다.
public record OrganizerWithdrawalEligibilityResponse(UUID organizerId, boolean hasActiveConference) {}
