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

// Reservation-Service가 직접 구현. secretKey는 응답에서 마스킹되어 돌아온다.
export function registerPgCredential({ provider, apiKey, secretKey }) {
  return apiFetch('/admin/settings/pg-key', { method: 'PATCH', body: { provider, apiKey, secretKey } })
}
