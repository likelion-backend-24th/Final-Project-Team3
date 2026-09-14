import { apiFetch } from './client'

export function login(email, password) {
  return apiFetch('/auth/login', { method: 'POST', body: { email, password } })
}

export function logout() {
  return apiFetch('/auth/logout', { method: 'POST' })
}

export function signupParticipant({ email, password, name, ageGroup, job }) {
  return apiFetch('/members/signup', { method: 'POST', body: { email, password, name, ageGroup, job } })
}

// 이메일 인증(#87): signup 자체는 바디가 그대로지만, 서버가 signup 시점에 해당 이메일이
// verify까지 완료됐는지 확인한다(MEMBER_EMAIL_NOT_VERIFIED). 그래서 프론트는 signup 전에
// 반드시 send-code → verify 순서로 먼저 호출해야 한다.
export function sendEmailCode(email) {
  return apiFetch('/auth/email/send-code', { method: 'POST', body: { email } })
}

export function verifyEmailCode(email, code) {
  return apiFetch('/auth/email/verify', { method: 'POST', body: { email, code } })
}

export function signupOrganizer({ email, password, name, organizationName, businessNo }) {
  return apiFetch('/members/organizers/signup', {
    method: 'POST',
    body: { email, password, name, organizationName, businessNo },
  })
}

// 마이페이지 프로필(연령대·직무) 조회/수정. 둘 다 로그인 필요(Bearer) —
// apiFetch가 accessToken을 자동으로 붙여준다.
export function getProfile() {
  return apiFetch('/members/me')
}

export function updateProfile({ ageGroup, job }) {
  return apiFetch('/members/me', { method: 'PATCH', body: { ageGroup, job } })
}

// JWT는 서명 검증 없이 payload만 디코드한다 — 화면 분기용이며 실제 인가는 서버가 매 요청마다 검증한다.
export function decodeJwt(token) {
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decodeURIComponent(escape(json)))
  } catch {
    return null
  }
}
