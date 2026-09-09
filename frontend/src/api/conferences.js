import { apiFetch } from './client'

// conference-service: PR #57/#65 머지로 organizerName/startAt/endAt/location/description/tags까지 실제로 저장된다.
export function listConferences() {
  return apiFetch('/conferences')
}

export function getConference(id) {
  return apiFetch(`/conferences/${id}`)
}

export function createConference({ organizerName, title, capacity, startAt, endAt, location, description, tags }) {
  return apiFetch('/conferences', {
    method: 'POST',
    body: { organizerName, title, capacity, startAt, endAt, location, description, tags },
  })
}

// 세션 "목록 조회"는 소유자 스코프 API가 아직 없어서(공개 GET /conferences/:id는 APPROVED 세션만 보여줌)
// 지금은 등록만 가능하다 — 세션 관리 화면 전체는 그 API가 생긴 뒤에 이어서 만든다.
export function createSession(conferenceId, { title, capacity, startAt, endAt }) {
  return apiFetch(`/conferences/${conferenceId}/sessions`, {
    method: 'POST',
    body: { title, capacity, startAt, endAt },
  })
}
