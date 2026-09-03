import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ChevronDown, Sun } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

// ⚠️ 미리보기 메뉴 — 주최자 회원가입이 아직 없어서(백엔드 Task 13-1 미구현) 화면만 보게 해주는 임시 스위처.
// 실제 로그인이 아니라 claims를 흉내내는 것뿐이라, API 응답이 필요한 데이터(대시보드 목록 등)는 비어있거나 에러로 보일 수 있다.
const PREVIEW_OPTIONS = [
  { role: null, label: '방문자로 보기' },
  { role: 'MEMBER', label: '참가자 화면 미리보기' },
  { role: 'ORGANIZER', label: '주최자 화면 미리보기' },
]

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
  const { status, claims, logout, previewRole, setPreviewRole } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const isOrganizer = claims?.role === 'ORGANIZER'
  const nav = isOrganizer ? organizerNav : participantNav(status === 'authenticated')
  const isRealAuth = status === 'authenticated' && !previewRole

  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef(null)

  useEffect(() => {
    if (!menuOpen) return
    const onClickOutside = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [menuOpen])

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
          {isRealAuth ? (
            <span className="inline-flex items-center gap-1 text-xs text-text-muted border border-border rounded-full px-3 py-1.5">
              {roleLabel[claims?.role] ?? '참가자'}
            </span>
          ) : (
            <div className="relative" ref={menuRef}>
              <button
                onClick={() => setMenuOpen((v) => !v)}
                className="inline-flex items-center gap-1 text-xs text-text-muted border border-border rounded-full px-3 py-1.5 hover:text-text hover:border-primary/40"
              >
                {previewRole ? `${roleLabel[previewRole]} (미리보기)` : '방문자'}
                <ChevronDown size={12} />
              </button>
              {menuOpen && (
                <div className="absolute right-0 mt-2 w-52 bg-surface border border-border rounded-lg py-1 shadow-lg z-20">
                  <p className="px-3 py-1.5 text-[11px] text-text-faint">
                    ⚠️ 화면 미리보기 — 실제 로그인 아님
                  </p>
                  {PREVIEW_OPTIONS.map((opt) => (
                    <button
                      key={opt.label}
                      onClick={() => {
                        setPreviewRole(opt.role)
                        setMenuOpen(false)
                        navigate(opt.role === 'ORGANIZER' ? '/organizer' : opt.role === 'MEMBER' ? '/my' : '/')
                      }}
                      className={`w-full text-left px-3 py-2 text-sm ${
                        previewRole === opt.role ? 'text-text bg-surface2' : 'text-text-muted hover:bg-surface2 hover:text-text'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
          {isRealAuth ? (
            <button
              onClick={handleLogout}
              title="로그아웃"
              className="w-9 h-9 rounded-full bg-primary/20 text-primary flex items-center justify-center text-sm font-semibold hover:bg-primary/30"
            >
              {claims?.email?.[0]?.toUpperCase() ?? '?'}
            </button>
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
