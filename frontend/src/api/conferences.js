import { apiFetch } from './client'

// conference-service: 컨퍼런스는 title/capacity/status만 가진다 (날짜·장소·발표자·이미지 등은 백엔드에 없음).
export function listConferences() {
  return apiFetch('/conferences')
}

export function getConference(id) {
  return apiFetch(`/conferences/${id}`)
}

export function createConference({ title, capacity }) {
  return apiFetch('/conferences', { method: 'POST', body: { title, capacity } })
}
