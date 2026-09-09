import { apiFetch } from './client'

// reservation-service엔 아직 JWT 필터가 없어서(SecurityConfig가 permitAll) memberId를 body로 직접 받는다.
export function createHold({ sessionId, memberId, headcount }) {
  return apiFetch('/reservations/hold', { method: 'POST', body: { sessionId, memberId, headcount } })
}

export function getQueuePosition(reservationId) {
  return apiFetch(`/reservations/${reservationId}/queue-position`)
}

// 결제는 아직 Mock이라 PG 없이 즉시 CONFIRMED 처리된다. Session에 가격 필드가 없어
// amount는 프론트에서 항상 0으로 보낸다.
export function submitPayment(reservationId, { paymentMethod, amount }) {
  return apiFetch(`/reservations/${reservationId}/payment`, {
    method: 'POST',
    body: { paymentMethod, amount },
  })
}

export function getQrTickets(reservationId) {
  return apiFetch(`/reservations/${reservationId}/qr-tickets`)
}
