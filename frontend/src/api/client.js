// 모든 백엔드 서비스가 { success, message, data, meta, error:{code,message}, traceId } 형태의
// ApiResponse로 응답한다 (member/conference/reservation-service 공통).
export class ApiError extends Error {
  constructor(code, message, status) {
    super(message)
    this.code = code
    this.status = status
  }
}

let accessToken = null
let onUnauthorized = null

export function setAccessToken(token) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

// 401 응답을 받았을 때(예: refresh 실패) 세션을 정리하기 위한 콜백. AuthContext가 등록한다.
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

async function parseResponse(res) {
  const text = await res.text()
  const body = text ? JSON.parse(text) : null
  return body
}

// refreshToken은 HttpOnly 쿠키라 JS로 못 만지고, 매 요청에 credentials:'include'로 자동 첨부된다.
// accessToken이 만료되어 401이 오면 /api/auth/refresh를 한 번 시도하고, 성공하면 원래 요청을 재시도한다.
export async function apiFetch(path, { method = 'GET', body, skipAuthRetry = false } = {}) {
  const headers = { }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    credentials: 'include',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  const parsed = await parseResponse(res)

  if (res.status === 401 && !skipAuthRetry && path !== '/auth/refresh') {
    const refreshed = await tryRefresh()
    if (refreshed) {
      return apiFetch(path, { method, body, skipAuthRetry: true })
    }
    onUnauthorized?.()
  }

  // 일부 엔드포인트(예: 세션 홀드 정원 초과)는 "정상적인 비즈니스 결과"를 4xx 상태 코드에
  // success:true 바디로 함께 내려준다(예: 대기열 등록 시 409). 그래서 에러 여부는 HTTP status가 아니라
  // 응답 바디의 success 필드로만 판단한다.
  if (!parsed || parsed.success !== true) {
    const err = parsed?.error
    throw new ApiError(err?.code ?? 'UNKNOWN', err?.message ?? '알 수 없는 오류가 발생했습니다.', res.status)
  }

  return parsed
}

async function tryRefresh() {
  try {
    const res = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' })
    const parsed = await parseResponse(res)
    if (res.ok && parsed?.success) {
      setAccessToken(parsed.data.accessToken)
      return true
    }
  } catch {
    // 네트워크 오류 등은 로그아웃 처리와 동일하게 취급
  }
  return false
}

export { tryRefresh }
