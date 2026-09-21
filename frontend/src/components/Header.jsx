import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Sun, LogOut } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

// 로그인 전엔 '마이페이지'가 없다 (본인 예약 내역이라 방문자에겐 의미 없음).
function participantNav(isAuthenticated) {
  return isAuthenticated
    ? [{ to: '/conferences', label: '컨퍼런스' }, { to: '/my', label: '마이페이지' }]
    : [{ to: '/conferences', label: '컨퍼런스' }]
}

const organizerNav = [
  { to: '/organizer', label: '대시보드', end: true },
  { to: '/organizer/conferences/new', label: '컨퍼런스 등록' },
  { to: '/organizer/applications', label: '신청 현황' },
  { to: '/organizer/checkin', label: 'QR 체크인' },
  { to: '/organizer/settlements', label: '정산 내역' },
]

const adminNav = [
  { to: '/admin', label: '컨퍼런스 승인', end: true },
  { to: '/admin/settlements', label: '정산 대시보드' },
  { to: '/admin/settings', label: '시스템 설정' },
]

const roleLabel = { MEMBER: '참가자', ORGANIZER: '주최자', ADMIN: '관리자' }

// '컨퍼런스' 메뉴는 홈("/")과 목록("/conferences")이 같은 화면이라 둘 다 active로 취급한다.
function isNavActive(item, pathname) {
  if (item.to === '/conferences') return pathname === '/' || pathname.startsWith('/conferences')
  if (item.end) return pathname === item.to
  return pathname === item.to || pathname.startsWith(`${item.to}/`)
}

export default function Header() {
  const { status, claims, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const isOrganizer = claims?.role === 'ORGANIZER'
  const isAdmin = claims?.role === 'ADMIN'
  const isAuthenticated = status === 'authenticated'
  const nav = isOrganizer ? organizerNav : isAdmin ? adminNav : participantNav(isAuthenticated)

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  // 모바일 가로 스크롤 메뉴에서 활성 항목이 화면 밖에 있지 않도록 이동 시 가운데로 맞춘다.
  useEffect(() => {
    document.querySelectorAll('[data-nav-active="true"]').forEach((el) => {
      el.scrollIntoView({ inline: 'center', block: 'nearest' })
    })
  }, [location.pathname, isOrganizer, isAdmin, isAuthenticated])

  const navLinks = nav.map((item) => (
    <Link
      key={item.to}
      to={item.to}
      data-nav-active={isNavActive(item, location.pathname)}
      className={`px-3 py-2 rounded-md text-sm border whitespace-nowrap transition-colors ${
        isNavActive(item, location.pathname)
          ? 'bg-surface2 border-border text-text'
          : 'border-transparent text-text-muted hover:text-text'
      }`}
    >
      {item.label}
    </Link>
  ))

  // 모바일(md 미만)에서는 메뉴를 로고 줄 아래의 가로 스크롤 줄로 내리고, 부가 요소(테마 토글·역할 뱃지)는 숨긴다.
  return (
    <header className="sticky top-0 z-10 bg-bg/95 backdrop-blur border-b border-border">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-14 sm:h-16 flex items-center justify-between gap-3">
        <div className="flex items-center gap-8 min-w-0">
          {/* 주최자는 로고를 눌러도 컨퍼런스 목록이 아니라 대시보드로 간다 */}
          <Link to={isOrganizer ? '/organizer' : '/'} className="text-lg font-semibold text-text shrink-0">
            Tech<span className="text-accent">Conf</span>
          </Link>
          <nav className="hidden md:flex items-center gap-1">{navLinks}</nav>
        </div>

        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          {/* 라이트 모드는 아직 구현 안 함 — 시안에 있는 토글 자리만 맞춰둔 상태 */}
          <button
            type="button"
            title="라이트 모드 (준비 중)"
            className="hidden sm:flex w-9 h-9 rounded-full border border-border text-text-muted items-center justify-center hover:text-text hover:border-primary/40"
          >
            <Sun size={16} />
          </button>

          <span className="hidden sm:inline-flex items-center gap-1 text-xs text-text-muted border border-border rounded-full px-3 py-1.5">
            {isAuthenticated ? (roleLabel[claims?.role] ?? '참가자') : '방문자'}
          </span>

          {isAuthenticated ? (
            <>
              <span
                title={claims?.email}
                className="w-9 h-9 rounded-full bg-primary/20 text-primary flex items-center justify-center text-sm font-semibold"
              >
                {claims?.email?.[0]?.toUpperCase() ?? '?'}
              </span>
              <button
                onClick={handleLogout}
                aria-label="로그아웃"
                className="inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text border border-border rounded-lg px-3 py-2"
              >
                <LogOut size={14} /> <span className="hidden sm:inline">로그아웃</span>
              </button>
            </>
          ) : (
            <Link to="/login">
              <button className="bg-primary hover:bg-primary-hover text-white text-sm font-medium px-4 py-2 rounded-lg whitespace-nowrap">
                로그인
              </button>
            </Link>
          )}
        </div>
      </div>

      <nav className="md:hidden border-t border-border overflow-x-auto">
        <div className="flex items-center gap-1 px-4 py-2 w-max">{navLinks}</div>
      </nav>
    </header>
  )
}
