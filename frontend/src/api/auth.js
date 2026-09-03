import { apiFetch } from './client'

export function login(email, password) {
  return apiFetch('/auth/login', { method: 'POST', body: { email, password } })
}

export function logout() {
  return apiFetch('/auth/logout', { method: 'POST' })
}

export function signupParticipant({ email, password, name }) {
  return apiFetch('/members/signup', { method: 'POST', body: { email, password, name } })
}

// 백엔드에 주최자 자체 회원가입 API(Task 13-1, #31)가 아직 구현되지 않았다.
// 계약은 GitHub #31 기준(email/password/name/organizationName/businessNo, POST /api/members/organizers/signup)으로
// 맞춰뒀고, 백엔드가 준비되면 바로 연결된다.
export function signupOrganizer({ email, password, name, organizationName, businessNo }) {
  return apiFetch('/members/organizers/signup', {
    method: 'POST',
    body: { email, password, name, organizationName, businessNo },
  })
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
