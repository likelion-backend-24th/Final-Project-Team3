import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import Button from '../components/Button'
import SelectField from '../components/SelectField'
import { useAuth } from '../context/AuthContext'
import { linkSocialAccount, withdrawMember } from '../api/auth'
import { ApiError } from '../api/client'
import { AGE_GROUPS, JOBS } from '../utils/profileOptions'
import { kakaoAuthorize, kakaoRedirectUri, decodeKakaoState } from '../utils/socialAuth'

// Kakao.Auth.authorize()가 페이지를 통째로 이 경로(kakaoRedirectUri())로 되돌려보낸다.
// 인가 코드(code)는 1회용이라, "추가 정보 필요" 상황이면 재시도가 아니라 값만 state에 실어서
// authorize()를 처음부터 다시 호출해야 한다(그래야 새 인가 코드를 받는다).
export default function KakaoCallback() {
  const { socialLogin, logout } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [status, setStatus] = useState('processing') // processing | profileRequired | error
  const [error, setError] = useState('')
  const [ageGroup, setAgeGroup] = useState('')
  const [job, setJob] = useState('')
  const [submitting, setSubmitting] = useState(false)
  // 인가 코드는 1회용인데 StrictMode가 개발 모드에서 effect를 두 번 실행시켜서, 가드 없이는
  // 같은 코드로 교환을 두 번 시도해 두 번째(또는 둘 다)가 실패한다.
  const handledRef = useRef(false)

  const routeAfterLogin = (claims) => {
    const dest = claims?.role === 'ORGANIZER' ? '/organizer' : claims?.role === 'ADMIN' ? '/admin' : '/conferences'
    navigate(dest, { replace: true })
  }

  useEffect(() => {
    if (handledRef.current) return
    handledRef.current = true

    const code = searchParams.get('code')
    const kakaoError = searchParams.get('error')
    const state = decodeKakaoState(searchParams.get('state') ?? '') ?? { intent: 'login' }

    if (kakaoError) {
      setStatus('error')
      setError('카카오 로그인이 취소됐어요.')
      return
    }
    if (!code) {
      setStatus('error')
      setError('잘못된 접근이에요.')
      return
    }

    const redirectUri = kakaoRedirectUri()

    if (state.intent === 'link') {
      linkSocialAccount('kakao', code, redirectUri)
        .then(() => navigate('/mypage', { replace: true, state: { kakaoLinked: true } }))
        .catch((err) => {
          setStatus('error')
          setError(err instanceof ApiError ? err.message : '계정 연동에 실패했습니다.')
        })
      return
    }

    if (state.intent === 'withdraw') {
      withdrawMember({ provider: 'kakao', socialToken: code, redirectUri })
        .then(() => logout())
        .then(() => navigate('/login', { replace: true, state: { withdrawDone: true } }))
        .catch((err) => {
          setStatus('error')
          setError(err instanceof ApiError ? err.message : '탈퇴에 실패했습니다.')
        })
      return
    }

    socialLogin('kakao', code, { redirectUri, ageGroup: state.ageGroup, job: state.job })
      .then((claims) => routeAfterLogin(claims))
      .catch((err) => {
        if (err instanceof ApiError && err.code === 'AUTH_SOCIAL_PROFILE_REQUIRED') {
          setStatus('profileRequired')
        } else {
          setStatus('error')
          setError(err instanceof ApiError ? err.message : '카카오 로그인에 실패했습니다.')
        }
      })
    // 최초 마운트 시 URL의 code/state로 한 번만 처리한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const submitProfile = (e) => {
    e.preventDefault()
    if (!ageGroup || !job) {
      setError('연령대와 직무를 선택해주세요.')
      return
    }
    setError('')
    setSubmitting(true)
    kakaoAuthorize({ intent: 'login', ageGroup, job }) // 새 인가 코드를 받기 위해 처음부터 다시 리다이렉트
  }

  if (status === 'profileRequired') {
    return (
      <div className="max-w-6xl mx-auto px-6 py-16">
        <div className="max-w-md mx-auto bg-surface border border-border rounded-xl p-6">
          <h1 className="text-lg font-semibold text-text mb-1">추가 정보 입력</h1>
          <p className="text-sm text-text-muted mb-6">
            처음 가입하시는군요! 연령대와 직무를 알려주시면 카카오 로그인을 한 번 더 진행할게요.
          </p>
          <form onSubmit={submitProfile} className="space-y-4">
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
            {error && <p className="text-sm text-danger">{error}</p>}
            <Button type="submit" variant="kakao" loading={submitting} className="w-full">
              Kakao로 계속하기
            </Button>
          </form>
        </div>
      </div>
    )
  }

  if (status === 'error') {
    return (
      <div className="max-w-6xl mx-auto px-6 py-16">
        <div className="max-w-md mx-auto bg-surface border border-border rounded-xl p-6 text-center">
          <p className="text-sm text-danger mb-4">{error}</p>
          <Link to="/login" className="text-accent text-sm">
            로그인 화면으로 돌아가기
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-16">
      <p className="text-center text-text-muted text-sm">카카오 로그인 처리 중...</p>
    </div>
  )
}
