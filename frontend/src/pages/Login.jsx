import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import TextField from '../components/TextField'
import SelectField from '../components/SelectField'
import Button from '../components/Button'
import { useAuth } from '../context/AuthContext'
import { ApiError } from '../api/client'
import { AGE_GROUPS, JOBS } from '../utils/profileOptions'
import { isGoogleConfigured, isKakaoConfigured, renderGoogleButton, kakaoLogin } from '../utils/socialAuth'

export default function Login() {
  const { login, socialLogin } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // 소셜 로그인이 AUTH_SOCIAL_PROFILE_REQUIRED로 튕기면 여기에 provider/token을 잠깐 들고 있다가
  // 연령대·직무를 받아서 같은 요청을 한 번 더 보낸다. 평소엔 null(=일반 로그인 화면).
  const [pendingSocial, setPendingSocial] = useState(null)
  const [ageGroup, setAgeGroup] = useState('')
  const [job, setJob] = useState('')
  const [socialLoading, setSocialLoading] = useState(false)
  const [socialError, setSocialError] = useState('')

  // 로컬에서 Google Cloud Console / Kakao Developers 앱을 아직 안 만들었을 때 쓰는 임시 입력값.
  // 백엔드 SOCIAL_MODE=mock이 토큰을 "email:이름" 문자열로 취급하는 것과 짝을 맞춤.
  const [mockEmail, setMockEmail] = useState('')
  const [mockName, setMockName] = useState('')

  const googleButtonRef = useRef(null)

  const routeAfterLogin = (claims) => {
    const dest = claims?.role === 'ORGANIZER' ? '/organizer' : claims?.role === 'ADMIN' ? '/admin' : '/conferences'
    navigate(dest)
  }

  useEffect(() => {
    if (isGoogleConfigured && googleButtonRef.current) {
      renderGoogleButton(googleButtonRef.current, (idToken) => handleSocialLogin('google', idToken))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const claims = await login(email, password)
      routeAfterLogin(claims)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  // provider: 'google' | 'kakao', token: 각 SDK(또는 mock 입력)가 준 idToken/accessToken
  const handleSocialLogin = async (provider, token) => {
    if (!token) return
    setSocialError('')
    setSocialLoading(true)
    try {
      const claims = await socialLogin(provider, token)
      routeAfterLogin(claims)
    } catch (err) {
      if (err instanceof ApiError && err.code === 'AUTH_SOCIAL_PROFILE_REQUIRED') {
        setPendingSocial({ provider, token }) // 최초 가입 — 추가 정보 입력 화면으로 전환
      } else {
        setSocialError(err instanceof ApiError ? err.message : '소셜 로그인에 실패했습니다.')
      }
    } finally {
      setSocialLoading(false)
    }
  }

  const handleKakaoClick = () => {
    setSocialError('')
    kakaoLogin(
      (accessToken) => handleSocialLogin('kakao', accessToken),
      () => setSocialError('카카오 로그인에 실패했습니다.'),
    )
  }

  const submitSocialProfile = async (e) => {
    e.preventDefault()
    if (!ageGroup || !job) {
      setSocialError('연령대와 직무를 선택해주세요.')
      return
    }
    setSocialError('')
    setSocialLoading(true)
    try {
      const claims = await socialLogin(pendingSocial.provider, pendingSocial.token, { ageGroup, job })
      routeAfterLogin(claims)
    } catch (err) {
      setSocialError(err instanceof ApiError ? err.message : '가입에 실패했습니다.')
    } finally {
      setSocialLoading(false)
    }
  }

  // 최초 소셜 가입 시 추가 정보 입력 화면
  if (pendingSocial) {
    return (
      <div className="max-w-6xl mx-auto px-6 py-16">
        <div className="max-w-md mx-auto bg-surface border border-border rounded-xl p-6">
          <h1 className="text-lg font-semibold text-text mb-1">추가 정보 입력</h1>
          <p className="text-sm text-text-muted mb-6">처음 가입하시는군요! 연령대와 직무만 알려주세요.</p>
          <form onSubmit={submitSocialProfile} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <SelectField
                label="연령대"
                placeholder="선택"
                options={AGE_GROUPS}
                value={ageGroup}
                onChange={(e) => setAgeGroup(e.target.value)}
                required
              />
              <SelectField
                label="직무"
                placeholder="선택"
                options={JOBS}
                value={job}
                onChange={(e) => setJob(e.target.value)}
                required
              />
            </div>
            {socialError && <p className="text-sm text-danger">{socialError}</p>}
            <Button type="submit" loading={socialLoading} className="w-full">
              가입 완료
            </Button>
          </form>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-16">
      <div className="max-w-md mx-auto">
        <div className="flex bg-surface2 rounded-lg p-1 mb-6">
          <span className="flex-1 text-center py-2 rounded-md bg-surface text-text text-sm font-medium">로그인</span>
          <Link
            to="/signup"
            className="flex-1 text-center py-2 rounded-md text-text-muted text-sm font-medium hover:text-text"
          >
            회원가입
          </Link>
        </div>

        <div className="bg-surface border border-border rounded-xl p-6">
          <h1 className="text-lg font-semibold text-text mb-6">로그인</h1>

          {location.state?.justSignedUp && (
            <p className="mb-4 text-sm text-success bg-success/10 rounded-lg px-3 py-2">
              회원가입이 완료됐어요. 로그인해주세요.
            </p>
          )}

          <form onSubmit={submit} className="space-y-4">
            <TextField
              label="이메일"
              type="email"
              placeholder="me@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <TextField
              label="비밀번호"
              type="password"
              placeholder="********"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            {error && <p className="text-sm text-danger">{error}</p>}
            <Button type="submit" loading={loading} className="w-full">
              로그인
            </Button>
          </form>

          <div className="flex items-center gap-3 my-5">
            <div className="flex-1 h-px bg-border" />
            <span className="text-xs text-text-muted">또는</span>
            <div className="flex-1 h-px bg-border" />
          </div>

          {/* 참석자 전용 — 주최자 로그인 화면에는 이 블록을 넣지 않는다 */}
          <div className="space-y-2">
            {isGoogleConfigured ? (
              <div ref={googleButtonRef} className="flex justify-center" />
            ) : (
              <Button
                variant="secondary"
                className="w-full"
                disabled
                title=".env에 VITE_GOOGLE_CLIENT_ID를 설정하면 활성화됩니다"
              >
                Google로 로그인 (미설정)
              </Button>
            )}

            {isKakaoConfigured ? (
              <Button variant="secondary" className="w-full" loading={socialLoading} onClick={handleKakaoClick}>
                Kakao로 로그인
              </Button>
            ) : (
              <Button
                variant="secondary"
                className="w-full"
                disabled
                title=".env에 VITE_KAKAO_JS_KEY를 설정하면 활성화됩니다"
              >
                Kakao로 로그인 (미설정)
              </Button>
            )}

            {socialError && <p className="text-sm text-danger">{socialError}</p>}
          </div>

          {/* 개발용 — 실제 Google/Kakao 앱을 아직 등록 안 했을 때 mock 로그인으로 흐름만 테스트 */}
          {import.meta.env.DEV && !isGoogleConfigured && !isKakaoConfigured && (
            <div className="mt-4 p-3 rounded-lg border border-dashed border-border">
              <p className="text-xs text-text-muted mb-2">
                개발용 mock 로그인 (백엔드 SOCIAL_MODE=mock 전용, 배포 전 제거)
              </p>
              <div className="flex gap-2">
                <input
                  className="flex-1 min-w-0 text-sm border border-border rounded-md px-2 py-1 bg-surface"
                  placeholder="mock 이메일"
                  value={mockEmail}
                  onChange={(e) => setMockEmail(e.target.value)}
                />
                <input
                  className="flex-1 min-w-0 text-sm border border-border rounded-md px-2 py-1 bg-surface"
                  placeholder="mock 이름"
                  value={mockName}
                  onChange={(e) => setMockName(e.target.value)}
                />
              </div>
              <div className="flex gap-2 mt-2">
                <Button
                  variant="secondary"
                  className="flex-1"
                  loading={socialLoading}
                  onClick={() => handleSocialLogin('google', `${mockEmail}:${mockName}`)}
                >
                  Google mock
                </Button>
                <Button
                  variant="secondary"
                  className="flex-1"
                  loading={socialLoading}
                  onClick={() => handleSocialLogin('kakao', `${mockEmail}:${mockName}`)}
                >
                  Kakao mock
                </Button>
              </div>
            </div>
          )}

          <p className="text-center text-sm text-text-muted mt-4">
            계정이 없으신가요? <Link to="/signup" className="text-accent">회원가입</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
