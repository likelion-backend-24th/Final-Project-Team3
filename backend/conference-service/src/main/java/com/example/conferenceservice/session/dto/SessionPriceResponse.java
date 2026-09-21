package com.example.conferenceservice.session.dto;

import com.example.conferenceservice.session.entity.Session;

import java.util.UUID;

/**
 * 세션 가격 조회 응답. Reservation-Service가 결제 검증 시 서버에서 결제 금액을 직접 계산하려고 호출하는
 * 서비스 간 내부 API(GET /api/sessions/{sessionId}/price)의 응답이라 Swagger에는 노출되지 않는다.
 *
 * @param sessionId 가격을 조회한 세션 ID
 * @param price     1인당 가격(원). 0이면 무료 세션이라 결제 없이 확정한다.
 *                  null이면 가격 미정이므로 Reservation-Service는 무료로 보지 않고 PortOne 검증을 거친다
 *                  (누락된 가격으로 결제를 우회하지 못하게 하는 fail-closed 규칙).
 */
public record SessionPriceResponse(
        UUID sessionId,
        Integer price
) {
    public static SessionPriceResponse from(Session session) {
        return new SessionPriceResponse(
                session.getId(),
                session.getPrice()
        );
    }
}
