import { apiFetch } from './client'

// reservation-service엔 아직 JWT 필터가 없어서(SecurityConfig가 permitAll) memberId를 body로 직접 받는다.
export function createHold({ sessionId, memberId, headcount }) {
  return apiFetch('/reservations/hold', { method: 'POST', body: { sessionId, memberId, headcount } })
}

export function getQueuePosition(reservationId) {
  return apiFetch(`/reservations/${reservationId}/queue-position`)
}
