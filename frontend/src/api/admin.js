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

// reservation-service 직접 구현(Task 18-3). 결제 건별 상세 — 세션ID·금액·예약상태·환불정보·
// 체크인 현황. 세션·컨퍼런스 이름은 안 내려줘서 프론트가 공개 컨퍼런스 목록으로 매핑해야 한다.
export function getSettlementDetails({ startDate, endDate, page, size } = {}) {
  const params = new URLSearchParams()
  if (startDate) params.set('startDate', startDate)
  if (endDate) params.set('endDate', endDate)
  if (page != null) params.set('page', page)
  if (size != null) params.set('size', size)
  const query = params.toString()
  return apiFetch(`/admin/settlements/details${query ? `?${query}` : ''}`)
}

// Reservation-Service가 직접 구현. 포트원(PortOne) 연동 값 — storeId·channelKey는 식별자,
// apiSecret·webhookSecret은 응답에서 마스킹되어 돌아온다.
export function registerPgCredential({ provider, storeId, channelKey, apiSecret, webhookSecret }) {
  return apiFetch('/admin/settings/pg-key', {
    method: 'PATCH',
    body: { provider, storeId, channelKey, apiSecret, webhookSecret },
  })
}

// Member-Service 직접 구현. 이메일·이름 키워드 검색 + 페이징되는 전체 유저 목록.
export function listMembers({ keyword, page, size } = {}) {
  const params = new URLSearchParams()
  if (keyword) params.set('keyword', keyword)
  if (page != null) params.set('page', page)
  if (size != null) params.set('size', size)
  const query = params.toString()
  return apiFetch(`/admin/members${query ? `?${query}` : ''}`)
}

export function getMemberDetail(memberId) {
  return apiFetch(`/admin/members/${memberId}`)
}

export function changeMemberRole(memberId, role) {
  return apiFetch(`/admin/members/${memberId}/role`, { method: 'PATCH', body: { role } })
}
