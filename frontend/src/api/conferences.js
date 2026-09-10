import { apiFetch } from './client'

// conference-service: organizerName은 이제 클라이언트가 안 보내도 서버가 JWT(주최자 조직명)로
// 채운다. tags는 최소 1개 필수(@NotEmpty), imageUrl은 선택.
export function listConferences() {
  return apiFetch('/conferences')
}

export function getConference(id) {
  return apiFetch(`/conferences/${id}`)
}

export function createConference({
  title,
  capacity,
  startAt,
  endAt,
  location,
  transportation,
  parkingInfo,
  amenities,
  description,
  imageUrl,
  tags,
}) {
  return apiFetch('/conferences', {
    method: 'POST',
    body: { title, capacity, startAt, endAt, location, transportation, parkingInfo, amenities, description, imageUrl, tags },
  })
}

// 주최자 소유 스코프로 "내 컨퍼런스의 세션 전체"(승인대기·반려 포함)를 조회한다.
// 공개 GET /conferences/:id는 APPROVED 세션만 주기 때문에 세션 관리 화면은 반드시 이걸 써야 한다.
export function getSessionsByConference(conferenceId) {
  return apiFetch(`/conferences/${conferenceId}/sessions`)
}

export function createSession(conferenceId, { title, capacity, startAt, endAt, sessionStartAt, sessionEndAt, location, speaker, price }) {
  return apiFetch(`/conferences/${conferenceId}/sessions`, {
    method: 'POST',
    body: { title, capacity, startAt, endAt, sessionStartAt, sessionEndAt, location, speaker, price },
  })
}

// 세션 수정은 정원·일정·장소·발표자·가격만 바꿀 수 있고, 백엔드가 수정 시마다 무조건
// status를 PENDING으로 리셋한다(재승인 정책) — 화면에서 그 사실을 안내해야 한다.
export function updateSession(sessionId, { capacity, startAt, endAt, sessionStartAt, sessionEndAt, location, speaker, price }) {
  return apiFetch(`/sessions/${sessionId}`, {
    method: 'PATCH',
    body: { capacity, startAt, endAt, sessionStartAt, sessionEndAt, location, speaker, price },
  })
}
