import { apiFetch } from './client'

// Story 21: 방문자·참가자가 보는 공개 프로필 — 인증 불필요.
// 승인(APPROVED)된 컨퍼런스만 포함되고, 컨퍼런스별 AI 요약(summaryText)은 아직 생성 전이면 null일 수 있다.
export function getOrganizerProfile(organizerId) {
  return apiFetch(`/organizers/${organizerId}/profile`)
}
