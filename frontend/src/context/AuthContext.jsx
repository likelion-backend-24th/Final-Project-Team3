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

export function AuthProvider({ children }) {
  const [claims, setClaims] = useState(null)
  const [status, setStatus] = useState('loading') // loading | authenticated | anonymous

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
    const res = await loginApi(email, password)
    applyToken(res.data.accessToken)
    setStatus('authenticated')
    return claimsFromToken(res.data.accessToken)
  }

  const logout = async () => {
    try {
      await logoutApi()
    } finally {
      clearSession()
    }
  }

  const value = { status, claims, login, logout, isAuthenticated: status === 'authenticated' }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth는 AuthProvider 안에서만 사용할 수 있습니다.')
  return ctx
}
