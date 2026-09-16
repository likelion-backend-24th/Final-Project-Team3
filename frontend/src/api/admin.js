import { apiFetch } from './client'

// 전체관리자 전용 API. Conference-Service가 직접 구현하고 Gateway가 /api/admin/**로 라우팅한다.
export function listPendingConferences() {
  return apiFetch('/admin/conferences')
}

export function getConferenceDetail(id) {
  return apiFetch(`/admin/conferences/${id}`)
}

export function approveConference(id) {
  return apiFetch(`/admin/conferences/${id}/approve`, { method: 'PATCH' })
}

export function rejectConference(id, reason) {
  return apiFetch(`/admin/conferences/${id}/reject`, { method: 'PATCH', body: { reason } })
}

export function listPendingSessions() {
  return apiFetch('/admin/sessions')
}

export function approveSession(id) {
  return apiFetch(`/admin/sessions/${id}/approve`, { method: 'PATCH' })
}

export function rejectSession(id, reason) {
  return apiFetch(`/admin/sessions/${id}/reject`, { method: 'PATCH', body: { reason } })
}

// reservation-service 직접 구현. 응답이 { totalAmount }뿐이라(주최자별/기간별 세부 집계 없음)
// 프론트도 그 이상은 못 보여준다. startDate/endDate는 선택(YYYY-MM-DD).
export function getSettlementDashboard({ startDate, endDate } = {}) {
  const params = new URLSearchParams()
  if (startDate) params.set('startDate', startDate)
  if (endDate) params.set('endDate', endDate)
  const query = params.toString()
  return apiFetch(`/admin/settlements${query ? `?${query}` : ''}`)
}

// reservation-service 직접 구현. provider/apiKey/secretKey만 받는다(webhook URL·수수료율은 계약에 없음).
export function updatePgCredential({ provider, apiKey, secretKey }) {
  return apiFetch('/admin/settings/pg-key', { method: 'PATCH', body: { provider, apiKey, secretKey } })
}
