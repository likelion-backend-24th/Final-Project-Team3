import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { QRCodeSVG } from 'qrcode.react'
import { Ticket, CheckCircle2, User, Briefcase, Link2 } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getMyReservations, getQueuePosition, getQrTickets, cancelReservation } from '../api/reservations'
import { listConferences, getConference } from '../api/conferences'
import { getProfile, updateProfile, linkSocialAccount, getLinkedSocialAccounts, withdrawMember } from '../api/auth'
import { ApiError } from '../api/client'
import Button from '../components/Button'
import GoogleIcon from '../components/GoogleIcon'
import KakaoIcon from '../components/KakaoIcon'
import SelectField from '../components/SelectField'
import TextField from '../components/TextField'
import { AGE_GROUPS, JOBS } from '../utils/profileOptions'
import { isGoogleConfigured, isKakaoConfigured, googleLogin, kakaoAuthorize } from '../utils/socialAuth'

const TABS = [
  { key: 'ALL', label: '전체' },
  { key: 'CONFIRMED', label: '확정' },
  { key: 'WAITING', label: '대기' },
  { key: 'CANCELLED', label: '취소' },
]

// HOLD(결제 대기)와 QUEUED(대기열)는 화면상 같은 "대기 중" 묶음으로 보여준다 — 둘 다
// 아직 좌석이 확정되지 않은 상태라는 점에서 참가자 입장에선 같은 범주다.
function categoryOf(status) {
  if (status === 'CONFIRMED') return 'CONFIRMED'
  if (status === 'CANCELLED') return 'CANCELLED'
  return 'WAITING'
}

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  return `${d.getMonth() + 1}월 ${d.getDate()}일 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const STATUS_STYLE = {
  CONFIRMED: { label: '확정', className: 'text-success bg-success/10' },
  CANCELLED: { label: '취소', className: 'text-text-muted bg-surface2' },
  WAITING: { label: '대기 중', className: 'text-warning bg-warning/10' },
}

export default function MyPage() {
  const { claims, logout } = useAuth()
  const navigate = useNavigate()
  const [reservations, setReservations] = useState(null)
  const [sessionMap, setSessionMap] = useState({})
  const [queuePositions, setQueuePositions] = useState({})
  const [tickets, setTickets] = useState({})
  const [expandedId, setExpandedId] = useState(null)
  const [tab, setTab] = useState('ALL')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [cancellingId, setCancellingId] = useState(null)

  // 프로필(연령대·직무) 수정 — ageGroup/job은 저장된 값(뱃지 표시용),
  // draftAgeGroup/draftJob은 "프로필 수정" 모드에서만 쓰는 편집 중 값
  const [ageGroup, setAgeGroup] = useState('')
  const [job, setJob] = useState('')
  const [editingProfile, setEditingProfile] = useState(false)
  const [draftAgeGroup, setDraftAgeGroup] = useState('')
  const [draftJob, setDraftJob] = useState('')
  const [profileError, setProfileError] = useState('')
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileSaved, setProfileSaved] = useState(false)

  // 소셜 계정 연동 — 마운트 시 서버에서 이미 연동된 Provider 목록을 조회해 상태를 채우고,
  // 화면에서 새로 연동하면 handleLinkSocial이 직접 상태를 갱신한다.
  const [googleLinkStatus, setGoogleLinkStatus] = useState('idle') // idle | linking | linked | error
  const [kakaoLinkStatus, setKakaoLinkStatus] = useState('idle')
  const [linkError, setLinkError] = useState('')
  const [mockLinkName, setMockLinkName] = useState('')

  // 회원 탈퇴 — hasPassword는 프로필 조회 결과로 채워지기 전까지 true(비밀번호 계정)로 가정해서
  // 소셜 재인증 UI가 잠깐 잘못 보이는 걸 막는다(더 안전한 쪽으로 기본값을 둠).
  const [hasPassword, setHasPassword] = useState(true)
  const [withdrawOpen, setWithdrawOpen] = useState(false)
  const [withdrawPassword, setWithdrawPassword] = useState('')
  const [withdrawing, setWithdrawing] = useState(false)
  const [withdrawError, setWithdrawError] = useState('')

  useEffect(() => {
    if (!claims?.memberId) return
    getLinkedSocialAccounts()
      .then((res) => {
        const providers = new Set(res.data.map((a) => a.provider))
        if (providers.has('GOOGLE')) setGoogleLinkStatus('linked')
        if (providers.has('KAKAO')) setKakaoLinkStatus('linked')
      })
      .catch(() => {
        // 조회 실패해도 연동 버튼은 그대로 눌러서 재시도할 수 있으니 화면을 막지 않는다
      })
  }, [claims?.memberId])

  useEffect(() => {
    if (!claims?.memberId) return
    getProfile()
      .then((res) => {
        setAgeGroup(res.data.ageGroup ?? '')
        setJob(res.data.job ?? '')
        setHasPassword(res.data.hasPassword)
      })
      .catch(() => {
        // 조회 실패해도 예약 목록은 정상 표시해야 하니, 프로필 칸만 빈 채로 둔다
      })
  }, [claims?.memberId])

  useEffect(() => {
    if (!claims?.memberId) return
    let cancelled = false

    getMyReservations(claims.memberId)
      .then((res) => {
        if (cancelled) return
        const list = res.data
        setReservations(list)

        // sessionId -> 세션(title/conferenceTitle/price/sessionStartAt) 매핑.
        // 세션 단건 공개 조회 API가 없어서, 공개 컨퍼런스 목록을 전부 훑어 세션을 찾는다.
        listConferences()
          .then((confRes) => Promise.all(confRes.data.map((c) => getConference(c.id))))
          .then((details) => {
            if (cancelled) return
            const map = {}
            details.forEach((d) => d.data.sessions.forEach((s) => { map[s.id] = s }))
            setSessionMap(map)
          })
          .catch(() => {})

        const queued = list.filter((r) => r.status === 'QUEUED')
        Promise.allSettled(queued.map((r) => getQueuePosition(r.reservationId))).then((results) => {
          if (cancelled) return
          const positions = {}
          results.forEach((r, i) => {
            if (r.status === 'fulfilled') positions[queued[i].reservationId] = r.value.data
          })
          setQueuePositions(positions)
        })
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : '예약 내역을 불러오지 못했습니다.'))

    return () => {
      cancelled = true
    }
  }, [claims?.memberId])

  const toggleTicket = async (reservationId) => {
    if (expandedId === reservationId) {
      setExpandedId(null)
      return
    }
    setExpandedId(reservationId)
    if (!tickets[reservationId]) {
      try {
        const res = await getQrTickets(reservationId)
        setTickets((t) => ({ ...t, [reservationId]: res.data?.[0] ?? null }))
      } catch {
        // 조회 실패해도 패널은 열어두고 QR 자리만 비워둔다
      }
    }
  }

  const handleCancel = async (r) => {
    const message = r.status === 'CONFIRMED'
      ? '예약을 취소할까요?\n세션 시작 7일 전까지 100%, 3~6일 전 50% 환불되고, 3일 미만이면 환불되지 않아요.'
      : '신청을 취소할까요?'
    if (!window.confirm(message)) return

    setError('')
    setNotice('')
    setCancellingId(r.reservationId)
    try {
      const res = await cancelReservation(r.reservationId)
      setReservations((list) => list.map((x) => (x.reservationId === r.reservationId ? { ...x, status: 'CANCELLED' } : x)))
      const { refundRate, refundAmount } = res.data
      setNotice(
        refundAmount != null
          ? `취소됐어요. 환불 ${refundAmount.toLocaleString()}원 (환불율 ${refundRate}%)`
          : '취소됐어요.',
      )
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '취소에 실패했습니다.')
    } finally {
      setCancellingId(null)
    }
  }

  const handleLinkSocial = async (provider, token) => {
    if (!token) return
    setLinkError('')
    const setStatus = provider === 'google' ? setGoogleLinkStatus : setKakaoLinkStatus
    setStatus('linking')
    try {
      await linkSocialAccount(provider, token)
      setStatus('linked')
    } catch (err) {
      setStatus('error')
      setLinkError(err instanceof ApiError ? err.message : '계정 연동에 실패했습니다.')
    }
  }

  // Kakao는 페이지 전체가 리다이렉트되므로, 결과는 이 화면이 아니라 KakaoCallback에서 처리하고
  // 성공하면 /mypage로 돌아온다(location.state.kakaoLinked로 확인 가능).
  const handleKakaoLinkClick = () => {
    kakaoAuthorize({ intent: 'link' })
  }

  const handleGoogleLinkClick = () => {
    setLinkError('')
    googleLogin((idToken) => handleLinkSocial('google', idToken))
  }

  // 탈퇴 성공 후엔 서버가 이미 Refresh Token을 전부 무효화했지만, 클라이언트 세션(accessToken 등)도
  // 같이 정리해야 해서 기존 logout()을 재사용한다 — 이미 폐기된 토큰을 한 번 더 폐기 시도하는 것뿐이라 안전하다.
  const finishWithdraw = async () => {
    await logout()
    navigate('/login', { state: { withdrawDone: true } })
  }

  const submitPasswordWithdraw = async (e) => {
    e.preventDefault()
    if (!window.confirm('정말 탈퇴하시겠어요? 이 작업은 되돌릴 수 없어요.')) return
    setWithdrawError('')
    setWithdrawing(true)
    try {
      await withdrawMember({ password: withdrawPassword })
      await finishWithdraw()
    } catch (err) {
      setWithdrawError(err instanceof ApiError ? err.message : '탈퇴에 실패했습니다.')
      setWithdrawing(false)
    }
  }

  const handleSocialWithdraw = async (provider, token) => {
    if (!token) return
    setWithdrawError('')
    setWithdrawing(true)
    try {
      await withdrawMember({ provider, socialToken: token })
      await finishWithdraw()
    } catch (err) {
      setWithdrawError(err instanceof ApiError ? err.message : '탈퇴에 실패했습니다.')
      setWithdrawing(false)
    }
  }

  const handleGoogleWithdrawClick = () => {
    if (!window.confirm('정말 탈퇴하시겠어요? 이 작업은 되돌릴 수 없어요.')) return
    setWithdrawError('')
    googleLogin((idToken) => handleSocialWithdraw('google', idToken))
  }

  // Kakao는 페이지 전체가 리다이렉트되므로, 결과는 KakaoCallback의 intent:'withdraw' 분기가 처리한다.
  const handleKakaoWithdrawClick = () => {
    if (!window.confirm('정말 탈퇴하시겠어요? 이 작업은 되돌릴 수 없어요.')) return
    kakaoAuthorize({ intent: 'withdraw' })
  }

  const startEditingProfile = () => {
    setDraftAgeGroup(ageGroup)
    setDraftJob(job)
    setProfileError('')
    setProfileSaved(false)
    setEditingProfile(true)
  }

  const saveProfile = async (e) => {
    e.preventDefault()
    setProfileError('')
    if (!draftAgeGroup || !draftJob) {
      setProfileError('연령대와 직무를 선택해주세요.')
      return
    }
    setProfileSaving(true)
    try {
      await updateProfile({ ageGroup: draftAgeGroup, job: draftJob })
      setAgeGroup(draftAgeGroup)
      setJob(draftJob)
      setEditingProfile(false)
      setProfileSaved(true)
    } catch (err) {
      setProfileError(err instanceof ApiError ? err.message : '프로필 저장에 실패했습니다.')
    } finally {
      setProfileSaving(false)
    }
  }

  const ageLabel = AGE_GROUPS.find((o) => o.value === ageGroup)?.label
  const jobLabel = JOBS.find((o) => o.value === job)?.label

  const filtered = useMemo(
    () => (reservations ?? []).filter((r) => tab === 'ALL' || categoryOf(r.status) === tab),
    [reservations, tab],
  )

  const counts = useMemo(() => {
    const list = reservations ?? []
    return {
      CONFIRMED: list.filter((r) => r.status === 'CONFIRMED').length,
      WAITING: list.filter((r) => categoryOf(r.status) === 'WAITING').length,
      CANCELLED: list.filter((r) => r.status === 'CANCELLED').length,
    }
  }, [reservations])

  const displayName = claims?.name ?? claims?.email?.split('@')[0] ?? '참가자'

  return (
    <div className="max-w-4xl mx-auto px-6 py-10">
      <div className="bg-surface border border-border rounded-xl p-5 mb-8">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4 min-w-0">
            <div className="w-14 h-14 rounded-full bg-primary/15 text-primary flex items-center justify-center text-xl font-semibold shrink-0">
              {displayName[0]?.toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="text-lg font-semibold text-text break-words">{displayName}</p>
              <p className="text-sm text-text-muted break-all">{claims?.email}</p>
            </div>
          </div>
          {!editingProfile && (
            <Button type="button" variant="secondary" className="self-start sm:self-auto" onClick={startEditingProfile}>
              프로필 수정
            </Button>
          )}
        </div>

        {editingProfile ? (
          <form onSubmit={saveProfile} className="mt-4 pt-4 border-t border-border">
            <div className="grid grid-cols-2 gap-4">
              <SelectField
                label="연령대"
                placeholder="선택"
                options={AGE_GROUPS}
                value={draftAgeGroup}
                onChange={(e) => setDraftAgeGroup(e.target.value)}
              />
              <SelectField
                label="직무"
                placeholder="선택"
                options={JOBS}
                value={draftJob}
                onChange={(e) => setDraftJob(e.target.value)}
              />
            </div>
            {profileError && <p className="text-sm text-danger mt-3">{profileError}</p>}
            <div className="flex items-center gap-3 mt-4">
              <Button type="submit" variant="primary" loading={profileSaving}>
                저장
              </Button>
              <Button type="button" variant="ghost" onClick={() => setEditingProfile(false)}>
                취소
              </Button>
            </div>
          </form>
        ) : (
          <div className="flex flex-wrap items-center gap-2 mt-4">
            {ageLabel && (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-surface2 border border-border text-sm text-text-muted">
                <User size={14} /> {ageLabel}
              </span>
            )}
            {jobLabel && (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-surface2 border border-border text-sm text-text-muted">
                <Briefcase size={14} /> {jobLabel}
              </span>
            )}
            {!ageLabel && !jobLabel && (
              <span className="text-sm text-text-muted">등록된 프로필 정보가 없습니다</span>
            )}
            {profileSaved && (
              <span className="inline-flex items-center gap-1 text-sm text-success">
                <CheckCircle2 size={16} /> 저장됐어요
              </span>
            )}
          </div>
        )}
      </div>

      <div className="bg-surface border border-border rounded-xl p-5 mb-8">
        <h2 className="text-sm font-semibold text-text mb-1">회원 탈퇴</h2>
        <p className="text-xs text-text-muted mb-4">
          탈퇴하면 계정 정보가 삭제되고 다시 로그인할 수 없게 돼요. 이 작업은 되돌릴 수 없어요.
        </p>

        {!withdrawOpen ? (
          <Button variant="secondary" onClick={() => setWithdrawOpen(true)}>
            탈퇴하기
          </Button>
        ) : hasPassword ? (
          <form onSubmit={submitPasswordWithdraw} className="space-y-3 max-w-sm">
            <TextField
              label="현재 비밀번호"
              type="password"
              placeholder="본인 확인을 위해 입력해주세요"
              value={withdrawPassword}
              onChange={(e) => setWithdrawPassword(e.target.value)}
              required
            />
            {withdrawError && <p className="text-sm text-danger">{withdrawError}</p>}
            <div className="flex gap-2">
              <Button type="submit" variant="danger" loading={withdrawing}>
                탈퇴 확정
              </Button>
              <Button type="button" variant="secondary" onClick={() => setWithdrawOpen(false)}>
                취소
              </Button>
            </div>
          </form>
        ) : (
          <div className="space-y-3">
            <p className="text-xs text-text-muted">
              소셜 로그인 전용 계정이라, 연동된 소셜 계정으로 본인 확인 후 탈퇴할 수 있어요.
            </p>
            <div className="flex items-center gap-2">
              {googleLinkStatus === 'linked' && (
                <Button
                  variant="google"
                  className="inline-flex items-center gap-1.5"
                  loading={withdrawing}
                  onClick={handleGoogleWithdrawClick}
                >
                  <GoogleIcon size={18} /> Google로 탈퇴
                </Button>
              )}
              {kakaoLinkStatus === 'linked' && (
                <Button
                  variant="kakao"
                  className="inline-flex items-center gap-1.5"
                  loading={withdrawing}
                  onClick={handleKakaoWithdrawClick}
                >
                  <KakaoIcon size={18} /> Kakao로 탈퇴
                </Button>
              )}
              <Button type="button" variant="secondary" onClick={() => setWithdrawOpen(false)}>
                취소
              </Button>
            </div>
            {withdrawError && <p className="text-sm text-danger">{withdrawError}</p>}
          </div>
        )}
      </div>

      <div className="bg-surface border border-border rounded-xl p-5 mb-8">
        <div className="flex items-center gap-2 mb-4">
          <Link2 size={16} className="text-text-muted" />
          <h2 className="text-sm font-semibold text-text">계정 연동</h2>
        </div>
        <p className="text-xs text-text-muted mb-4">
          비밀번호 계정에 소셜 계정을 연동하면, 다음부터는 소셜 로그인 버튼만으로 같은 계정에 들어올 수 있어요.
          연동하려는 소셜 계정의 이메일은 지금 로그인된 이메일({claims?.email})과 같아야 해요.
        </p>

        <div className="flex items-center gap-2">
          {googleLinkStatus === 'linked' ? (
            <span className="inline-flex items-center gap-1 text-sm text-success px-2">
              <CheckCircle2 size={16} /> Google 연동됨
            </span>
          ) : isGoogleConfigured ? (
            <Button
              variant="google"
              className="inline-flex items-center gap-1.5"
              loading={googleLinkStatus === 'linking'}
              onClick={handleGoogleLinkClick}
            >
              <GoogleIcon size={18} /> Google
            </Button>
          ) : (
            <Button variant="secondary" className="inline-flex items-center gap-1.5" disabled title=".env에 VITE_GOOGLE_CLIENT_ID를 설정하면 활성화됩니다">
              <GoogleIcon size={18} /> Google
            </Button>
          )}

          {kakaoLinkStatus === 'linked' ? (
            <span className="inline-flex items-center gap-1 text-sm text-success px-2">
              <CheckCircle2 size={16} /> Kakao 연동됨
            </span>
          ) : isKakaoConfigured ? (
            <Button
              variant="kakao"
              className="inline-flex items-center gap-1.5"
              loading={kakaoLinkStatus === 'linking'}
              onClick={handleKakaoLinkClick}
            >
              <KakaoIcon size={18} /> Kakao
            </Button>
          ) : (
            <Button variant="secondary" className="inline-flex items-center gap-1.5" disabled title=".env에 VITE_KAKAO_JS_KEY를 설정하면 활성화됩니다">
              <KakaoIcon size={18} /> Kakao
            </Button>
          )}
        </div>

        {linkError && <p className="text-sm text-danger mt-3">{linkError}</p>}

        {/* 개발용 — 실제 Google/Kakao 앱을 아직 등록 안 했을 때 mock 연동으로 흐름만 테스트 */}
        {import.meta.env.DEV && !isGoogleConfigured && !isKakaoConfigured && (
          <div className="mt-4 p-3 rounded-lg border border-dashed border-border">
            <p className="text-xs text-text-muted mb-2">
              개발용 mock 연동 (백엔드 SOCIAL_MODE=mock 전용, 배포 전 제거) — 이메일은 자동으로 본인 이메일이 들어가요.
            </p>
            <input
              className="w-full text-sm border border-border rounded-md px-2 py-1 bg-surface mb-2"
              placeholder="mock 이름"
              value={mockLinkName}
              onChange={(e) => setMockLinkName(e.target.value)}
            />
            <div className="flex gap-2">
              <Button
                variant="secondary"
                className="flex-1"
                loading={googleLinkStatus === 'linking'}
                onClick={() => handleLinkSocial('google', `${claims?.email}:${mockLinkName}`)}
              >
                Google mock 연동
              </Button>
              <Button
                variant="secondary"
                className="flex-1"
                loading={kakaoLinkStatus === 'linking'}
                onClick={() => handleLinkSocial('kakao', `${claims?.email}:${mockLinkName}`)}
              >
                Kakao mock 연동
              </Button>
            </div>
          </div>
        )}
      </div>

      <div className="grid grid-cols-3 gap-3 sm:gap-4 mb-8">
        <div className="bg-surface border border-border rounded-xl p-3.5 sm:p-5">
          <p className="flex items-center gap-1.5 text-xs sm:text-sm text-text-muted mb-1.5 whitespace-nowrap">
            <span className="w-1.5 h-1.5 rounded-full bg-success" /> 예약 확정
          </p>
          <p className="text-2xl font-semibold text-success">{counts.CONFIRMED}</p>
        </div>
        <div className="bg-surface border border-border rounded-xl p-3.5 sm:p-5">
          <p className="flex items-center gap-1.5 text-xs sm:text-sm text-text-muted mb-1.5 whitespace-nowrap">
            <span className="w-1.5 h-1.5 rounded-full bg-warning" /> 대기 중
          </p>
          <p className="text-2xl font-semibold text-warning">{counts.WAITING}</p>
        </div>
        <div className="bg-surface border border-border rounded-xl p-3.5 sm:p-5">
          <p className="flex items-center gap-1.5 text-xs sm:text-sm text-text-muted mb-1.5 whitespace-nowrap">
            <span className="w-1.5 h-1.5 rounded-full bg-text-faint" /> 취소
          </p>
          <p className="text-2xl font-semibold text-text-muted">{counts.CANCELLED}</p>
        </div>
      </div>

      <div className="flex gap-1 bg-surface2 rounded-lg p-1 mb-6 w-fit">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              tab === t.key ? 'bg-surface text-text shadow-sm' : 'text-text-muted hover:text-text'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {error && <p className="text-sm text-danger mb-4">{error}</p>}
      {notice && <p className="text-sm text-success mb-4">{notice}</p>}
      {!reservations && !error && <p className="text-text-muted text-sm">불러오는 중...</p>}

      <div className="space-y-4">
        {filtered.map((r) => {
          const session = sessionMap[r.sessionId]
          const amount = session?.price ? session.price * r.headcount : 0
          const ticket = tickets[r.reservationId]
          const isOpen = expandedId === r.reservationId
          const status = STATUS_STYLE[categoryOf(r.status)]

          return (
            <div key={r.reservationId} className="bg-surface border border-border rounded-xl overflow-hidden">
              <div className="p-5">
                <div className="flex items-start justify-between gap-3 mb-4">
                  <div>
                    <p className="text-text font-medium">{session?.title ?? `세션 #${r.sessionId.slice(0, 8)}`}</p>
                    <p className="text-sm text-text-muted mt-0.5">{session?.conferenceTitle ?? '-'}</p>
                  </div>
                  <span className={`inline-flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full shrink-0 ${status.className}`}>
                    <span className="w-1.5 h-1.5 rounded-full bg-current" />
                    {status.label}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-4 pb-4 border-b border-border text-sm">
                  <div>
                    <p className="text-text-faint mb-1">일시</p>
                    <p className="text-text">{formatDateTime(session?.sessionStartAt)}</p>
                  </div>
                  {r.status === 'QUEUED' ? (
                    <div>
                      <p className="text-text-faint mb-1">대기 순번</p>
                      <p className="text-warning font-medium">
                        {queuePositions[r.reservationId] != null ? `${queuePositions[r.reservationId]}번` : '-'}
                      </p>
                    </div>
                  ) : (
                    <div>
                      <p className="text-text-faint mb-1">결제 금액</p>
                      <p className="text-text font-medium">{amount > 0 ? `${amount.toLocaleString()}원` : '무료'}</p>
                    </div>
                  )}
                </div>

                <div className="flex gap-2 mt-4">
                  {r.status === 'CONFIRMED' && (
                    <Button variant="secondary" className="inline-flex items-center gap-1.5" onClick={() => toggleTicket(r.reservationId)}>
                      <Ticket size={15} /> {isOpen ? '닫기' : 'QR 티켓'}
                    </Button>
                  )}
                  {r.status === 'HOLD' && (
                    <Link
                      to={`/reservations/${r.reservationId}/payment`}
                      state={{ sessionTitle: session?.title, conferenceTitle: session?.conferenceTitle, headcount: r.headcount, price: session?.price ?? 0 }}
                    >
                      <Button variant="secondary">결제하러 가기</Button>
                    </Link>
                  )}
                  {r.status !== 'CANCELLED' && (
                    <Button variant="ghost" loading={cancellingId === r.reservationId} onClick={() => handleCancel(r)}>
                      취소·환불
                    </Button>
                  )}
                </div>
              </div>

              {isOpen && (
                <div className="bg-bg border-t border-border p-5 flex items-center justify-between gap-4">
                  <div>
                    <p className="text-xs text-text-faint tracking-wide mb-1">TECHCONF · 입장권</p>
                    <p className="text-text font-medium mb-3">{session?.title ?? '세션'}</p>
                    <p className="text-xs text-text-faint">일시</p>
                    <p className="text-sm text-text mb-2">{formatDateTime(session?.sessionStartAt)}</p>
                    <p className="text-xs text-text-faint">금액</p>
                    <p className="text-sm text-text">{amount > 0 ? `${amount.toLocaleString()}원` : '무료'}</p>
                    {ticket && <p className="text-xs text-text-faint font-mono mt-3">{ticket.code}</p>}
                  </div>
                  {ticket ? (
                    <QRCodeSVG value={ticket.code} size={96} />
                  ) : (
                    <div className="w-24 h-24 rounded-lg bg-surface2 animate-pulse shrink-0" />
                  )}
                </div>
              )}
            </div>
          )
        })}
        {reservations && filtered.length === 0 && (
          <p className="text-text-faint text-sm py-12 text-center">해당하는 예약이 없어요.</p>
        )}
      </div>
    </div>
  )
}
