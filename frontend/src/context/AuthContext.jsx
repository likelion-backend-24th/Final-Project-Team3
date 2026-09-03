import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { getAccessToken, setAccessToken, setUnauthorizedHandler, tryRefresh } from '../api/client'
import { login as loginApi, logout as logoutApi, decodeJwt } from '../api/auth'

const AuthContext = createContext(null)

// role: 'MEMBER' | 'ORGANIZER' | 'ADMIN' | null(방문자)
function claimsFromToken(token) {
  if (!token) return null
  const payload = decodeJwt(token)
  if (!payload) return null
  return {
    memberId: payload.sub,
    email: payload.email,
    role: payload.role,
    organizerId: payload.organizerId ?? null,
  }
}

// ⚠️ 화면 미리보기 전용 더미 claims — 실제 로그인이 아니다.
// 주최자 회원가입이 아직 백엔드에 없어서(Task 13-1, #31 미구현) 화면만 확인할 수 있게 만든 임시 우회 장치.
// 실제 API 호출(예: 대시보드 컨퍼런스 조회)은 이 memberId/organizerId가 실존하지 않아 빈 값/에러로 나올 수 있다.
const PREVIEW_CLAIMS = {
  MEMBER: { memberId: 'preview-member', email: 'preview-member@techconf.dev', role: 'MEMBER', organizerId: null },
  ORGANIZER: { memberId: 'preview-organizer', email: 'preview-organizer@techconf.dev', role: 'ORGANIZER', organizerId: 'preview-organizer' },
}

export function AuthProvider({ children }) {
  const [claims, setClaims] = useState(null)
  const [status, setStatus] = useState('loading') // loading | authenticated | anonymous
  const [previewRole, setPreviewRole] = useState(null) // null | 'MEMBER' | 'ORGANIZER' — 미리보기 모드

  const applyToken = (token) => {
    setAccessToken(token)
    setClaims(claimsFromToken(token))
  }

  const clearSession = useCallback(() => {
    setAccessToken(null)
    setClaims(null)
    setStatus('anonymous')
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(clearSession)
    // refreshToken은 HttpOnly 쿠키로만 존재하므로, 새로고침 시 여기서 조용히 재발급을 시도해 세션을 복구한다.
    ;(async () => {
      const ok = await tryRefresh()
      if (ok) {
        applyToken(getAccessToken())
        setStatus('authenticated')
      } else {
        setStatus('anonymous')
      }
    })()
  }, [clearSession])

  const login = async (email, password) => {
    setPreviewRole(null) // 실제 로그인하면 미리보기 모드는 해제
    const res = await loginApi(email, password)
    applyToken(res.data.accessToken)
    setStatus('authenticated')
    return claimsFromToken(res.data.accessToken)
  }

  const logout = async () => {
    setPreviewRole(null)
    try {
      await logoutApi()
    } finally {
      clearSession()
    }
  }

  // 미리보기 모드일 땐 실제 인증 없이 claims/status를 흉내낸다 (컴포넌트 쪽 코드는 몰라도 됨).
  const value = {
    status: previewRole ? 'authenticated' : status,
    claims: previewRole ? PREVIEW_CLAIMS[previewRole] : claims,
    login,
    logout,
    isAuthenticated: previewRole ? true : status === 'authenticated',
    previewRole,
    setPreviewRole,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth는 AuthProvider 안에서만 사용할 수 있습니다.')
  return ctx
}
