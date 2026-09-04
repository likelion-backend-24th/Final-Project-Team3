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
  { to: '/organizer/operations', label: '운영 현황' },
  { to: '/organizer/checkin', label: 'QR 체크인' },
  { to: '/organizer/settlements', label: '정산 내역' },
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
  const isAuthenticated = status === 'authenticated'
  const nav = isOrganizer ? organizerNav : participantNav(isAuthenticated)

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-10 bg-bg/95 backdrop-blur border-b border-border">
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        <div className="flex items-center gap-8">
          {/* 주최자는 로고를 눌러도 컨퍼런스 목록이 아니라 대시보드로 간다 */}
          <Link to={isOrganizer ? '/organizer' : '/'} className="text-lg font-semibold text-text">
            Tech<span className="text-accent">Conf</span>
          </Link>
          <nav className="flex items-center gap-1">
            {nav.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className={`px-3 py-2 rounded-md text-sm border transition-colors ${
                  isNavActive(item, location.pathname)
                    ? 'bg-surface2 border-border text-text'
                    : 'border-transparent text-text-muted hover:text-text'
                }`}
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </div>

        <div className="flex items-center gap-3">
          {/* 라이트 모드는 아직 구현 안 함 — 시안에 있는 토글 자리만 맞춰둔 상태 */}
          <button
            type="button"
            title="라이트 모드 (준비 중)"
            className="w-9 h-9 rounded-full border border-border text-text-muted flex items-center justify-center hover:text-text hover:border-primary/40"
          >
            <Sun size={16} />
          </button>

          <span className="inline-flex items-center gap-1 text-xs text-text-muted border border-border rounded-full px-3 py-1.5">
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
                className="inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text border border-border rounded-lg px-3 py-2"
              >
                <LogOut size={14} /> 로그아웃
              </button>
            </>
          ) : (
            <Link to="/login">
              <button className="bg-primary hover:bg-primary-hover text-white text-sm font-medium px-4 py-2 rounded-lg">
                로그인
              </button>
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
