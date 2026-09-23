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

// 소셜 로그인/가입. 최초 가입일 때만 서버가 AUTH_SOCIAL_PROFILE_REQUIRED(400)로 ageGroup·job을 요구한다.
// redirectUri는 Kakao(authorize 인가 코드 교환)에서만 필요하고 Google은 무시된다.
export function socialLogin(provider, token, { ageGroup, job, redirectUri } = {}) {
  return apiFetch(`/auth/social/${provider}`, {
    method: 'POST',
    body: { token, ageGroup, job, redirectUri },
  })
}

// 마이페이지에서 이미 로그인된 계정에 소셜 계정을 연동할 때(이메일 충돌 케이스 해소용)
export function linkSocialAccount(provider, token, redirectUri) {
  return apiFetch(`/auth/social/${provider}/link`, { method: 'POST', body: { token, redirectUri } })
}

// 본인 계정에 연동된 소셜 Provider 목록 조회
export function getLinkedSocialAccounts() {
  return apiFetch('/members/me/social-accounts')
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
