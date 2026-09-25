import { apiFetch, apiDownload } from './client'

// conference-service: organizerName은 이제 클라이언트가 안 보내도 서버가 JWT(주최자 조직명)로
// 채운다. tags는 최소 1개 필수(@NotEmpty), image는 선택.
export function listConferences() {
  return apiFetch('/conferences')
}

// 주최자 본인 소유 컨퍼런스를 상태 무관(PENDING/APPROVED/REJECTED)으로 조회한다.
// 공개 GET /conferences는 APPROVED만 주기 때문에, 주최자 대시보드는 반드시 이걸 써야 승인 대기 중인
// 자기 컨퍼런스도 볼 수 있다.
export function getMyConferences() {
  return apiFetch('/conferences/my')
}

export function getConference(id) {
  return apiFetch(`/conferences/${id}`)
}

// 체크인 완료 참석자의 연령대·직무 분포 + AI 요약(Story 15). 소유 주최자만 조회 가능(403).
export function getAttendeeSummary(conferenceId) {
  return apiFetch(`/conferences/${conferenceId}/attendee-summary`)
}

// 체크인 완료 예약의 후기 원문 목록(작성자 식별 정보 없음). AI 요약과 별개로 주최자가 직접 훑어볼 수 있게.
export function getReviews(conferenceId) {
  return apiFetch(`/conferences/${conferenceId}/reviews`)
}

// 컨퍼런스 단건 정산(매출/환불/순매출/확정·취소 건수). 소유 주최자만 조회 가능(OwnerScopeGuard).
export function getSettlement(conferenceId) {
  return apiFetch(`/conferences/${conferenceId}/settlement`)
}

// 세션별 신청·입장 현황(hold/대기/확정/취소/체크인 건수). 소유 주최자만 조회 가능(OwnerScopeGuard).
export function getOperationStatus(conferenceId) {
  return apiFetch(`/conferences/${conferenceId}/operation-status`)
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
  tags,
  proofFile,
  image,
}) {
  // 백엔드는 multipart/form-data만 받는다: JSON 본문은 "request" 파트, 증빙 파일은 선택 "proofFile" 파트,
  // 대표 이미지는 선택 "image" 파트. 서버가 업로드 시점에 썸네일/상세용으로 각각 리사이징해서 저장한다.
  const form = new FormData()
  form.append(
    'request',
    new Blob(
      [JSON.stringify({ title, capacity, startAt, endAt, location, transportation, parkingInfo, amenities, description, tags })],
      { type: 'application/json' },
    ),
  )
  if (proofFile) form.append('proofFile', proofFile)
  if (image) form.append('image', image)
  return apiFetch('/conferences', { method: 'POST', body: form })
}

// 소개글 본문에 삽입할 이미지를 업로드하고 URL을 돌려받는다(컨퍼런스 생성/수정과 별개 요청).
// 받은 URL은 소개글 텍스트 안에 `![alt](url)` 마크다운 문법으로 그대로 붙여넣는다.
export function uploadDescriptionImage(file) {
  const form = new FormData()
  form.append('image', file)
  return apiFetch('/conferences/description-images', { method: 'POST', body: form })
}

// 승인 심사용 증빙 파일. 소유 주최자 본인과 관리자만 받을 수 있다(403). 첨부된 컨퍼런스에만 호출할 것.
export function downloadProofFile(conferenceId) {
  return apiDownload(`/conferences/${conferenceId}/proof-file`)
}

// 주최자 소유 스코프로 "내 컨퍼런스의 세션 전체"(승인대기·반려 포함)를 조회한다.
// 공개 GET /conferences/:id는 APPROVED 세션만 주기 때문에 세션 관리 화면은 반드시 이걸 써야 한다.
export function getSessionsByConference(conferenceId) {
  return apiFetch(`/conferences/${conferenceId}/sessions`)
}

// maxHeadcountPerApplication(1인당 최대 신청 인원)은 백엔드가 @NotNull로 요구한다 — 안 보내면 400.
export function createSession(conferenceId, { title, capacity, startAt, endAt, sessionStartAt, sessionEndAt, location, speaker, price, maxHeadcountPerApplication }) {
  return apiFetch(`/conferences/${conferenceId}/sessions`, {
    method: 'POST',
    body: { title, capacity, startAt, endAt, sessionStartAt, sessionEndAt, location, speaker, price, maxHeadcountPerApplication },
  })
}

// 세션 수정은 정원·일정·장소·발표자·가격·1인당 최대 신청 인원만 바꿀 수 있고, 백엔드가 수정 시마다
// 무조건 status를 PENDING으로 리셋한다(재승인 정책) — 화면에서 그 사실을 안내해야 한다.
export function updateSession(sessionId, { capacity, startAt, endAt, sessionStartAt, sessionEndAt, location, speaker, price, maxHeadcountPerApplication }) {
  return apiFetch(`/sessions/${sessionId}`, {
    method: 'PATCH',
    body: { capacity, startAt, endAt, sessionStartAt, sessionEndAt, location, speaker, price, maxHeadcountPerApplication },
  })
}

// 컨퍼런스 설정 - 소개글. 승인 여부와 무관하게 수정 가능.
export function updateDescription(conferenceId, description) {
  return apiFetch(`/conferences/${conferenceId}/description`, {
    method: 'PATCH',
    body: { description },
  })
}

// 컨퍼런스 설정 - 장소. 승인된 컨퍼런스는 주소(location)만 잠기고 교통편/주차/편의시설은 계속 수정 가능
// (백엔드가 location 값이 실제로 바뀔 때만 막는다 — 그 외 필드엔 제한이 없다).
export function updateLocation(conferenceId, { location, transportation, parkingInfo, amenities }) {
  return apiFetch(`/conferences/${conferenceId}/location`, {
    method: 'PATCH',
    body: { location, transportation, parkingInfo, amenities },
  })
}

// 컨퍼런스 설정 - 공지/안내. 조회는 공개, 등록/삭제는 소유 주최자만(백엔드가 OwnerScopeGuard로 검증).
export function listNotices(conferenceId) {
  return apiFetch(`/conferences/${conferenceId}/notices`)
}
export function createNotice(conferenceId, { title, content }) {
  return apiFetch(`/conferences/${conferenceId}/notices`, {
    method: 'POST',
    body: { title, content },
  })
}
export function deleteNotice(conferenceId, noticeId) {
  return apiFetch(`/conferences/${conferenceId}/notices/${noticeId}`, { method: 'DELETE' })
}

// 컨퍼런스 설정 - 문의사항(FAQ). 조회는 공개, 등록/삭제는 소유 주최자만.
export function listFaqs(conferenceId) {
  return apiFetch(`/conferences/${conferenceId}/faqs`)
}
export function createFaq(conferenceId, { question, answer }) {
  return apiFetch(`/conferences/${conferenceId}/faqs`, {
    method: 'POST',
    body: { question, answer },
  })
}
export function deleteFaq(conferenceId, faqId) {
  return apiFetch(`/conferences/${conferenceId}/faqs/${faqId}`, { method: 'DELETE' })
}
