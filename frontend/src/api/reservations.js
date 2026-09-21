import { apiFetch } from './client'

// reservation-service엔 아직 JWT 필터가 없어서(SecurityConfig가 permitAll) memberId를 직접 넘긴다.
// 로그인한 사람과 memberId가 실제로 일치하는지는 서버가 검증 안 해준다 — 남은 보안 갭.
// attendees(좌석별 연령대·직무, 요구사항 v0.5)는 백엔드 계약이 아직 없어서(Task 9-5 필요)
// 지금은 보내도 서버가 무시한다 — 계약이 생기면 그대로 살아날 자리만 미리 만들어둔 것.
export function createHold({ sessionId, memberId, headcount, attendees }) {
  return apiFetch('/reservations/hold', { method: 'POST', body: { sessionId, memberId, headcount, attendees } })
}

export function getQueuePosition(reservationId) {
  return apiFetch(`/reservations/${reservationId}/queue-position`)
}

// PortOne 결제창(SDK) 초기화에 필요한 공개 설정. apiSecret/webhookSecret은 응답에 없다.
export function getPgConfig() {
  return apiFetch('/payments/pg-config')
}

// paymentId는 PortOne SDK로 결제창을 띄울 때 쓴 값(=reservationId 문자열)을 그대로 전달한다.
// amount는 서버가 세션 가격×인원으로 직접 계산해서 PortOne에 재검증하므로 여기서 보내지 않는다.
export function submitPayment(reservationId, { paymentId }) {
  return apiFetch(`/reservations/${reservationId}/payment`, {
    method: 'POST',
    body: { paymentId },
  })
}

// 응답은 {reservationId, status, refundRate, refundAmount} — HOLD/QUEUED 취소는 환불이 없어 두 값이 null이다.
export function cancelReservation(reservationId) {
  return apiFetch(`/reservations/${reservationId}/cancel`, { method: 'POST' })
}

export function getQrTickets(reservationId) {
  return apiFetch(`/qr-tickets/${reservationId}`)
}

// QR 코드 문자열(발급 시 대시 없는 UUID)로 입장 처리. 응답은 {code, used, usedAt}뿐 -
// 참가자 이름/세션 정보는 이 엔드포인트가 안 줘서 프론트에서 못 붙인다.
export function scanQrTicket(code) {
  return apiFetch(`/qr-tickets/${code}/scan`, { method: 'POST' })
}

// 내 예약 목록. memberId를 서버가 JWT로 채우는 게 아니라 쿼리로 그대로 받는다(위와 같은 갭).
// 응답은 reservationId/sessionId/status/headcount/createdAt뿐이라 세션 제목 등은 따로 조합해야 한다.
export function getMyReservations(memberId) {
  return apiFetch(`/reservations/my?memberId=${memberId}`)
}

// 세션의 실제 확정 인원·잔여좌석. capacity만 아는 conference-service 쪽 API와 달리
// reservation-service의 session_capacity_lock 기준 실데이터를 준다.
export function getCapacityStatus(sessionId) {
  return apiFetch(`/reservations/sessions/${sessionId}/capacity-status`)
}
