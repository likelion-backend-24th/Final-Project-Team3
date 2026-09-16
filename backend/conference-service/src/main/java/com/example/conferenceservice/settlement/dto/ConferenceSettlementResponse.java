package com.example.conferenceservice.settlement.dto;

import java.util.UUID;

public record ConferenceSettlementResponse(
        UUID conferenceId,
        String conferenceTitle,
        int totalRevenue,
        int refundedAmount,
        int netRevenue,
        int confirmedCount,
        int cancelledCount
) {}
