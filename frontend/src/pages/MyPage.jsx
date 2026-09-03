import { useAuth } from '../context/AuthContext'

export default function MyPage() {
  const { claims } = useAuth()

  return (
    <div className="max-w-6xl mx-auto px-6 py-16">
      <h1 className="text-2xl font-semibold text-text mb-6">마이페이지</h1>

      <div className="max-w-md bg-surface border border-border rounded-xl p-5 mb-6">
        <p className="text-sm text-text-muted mb-1">이메일</p>
        <p className="text-text mb-4">{claims?.email}</p>
        <p className="text-sm text-text-muted mb-1">역할</p>
        <p className="text-text">참가자</p>
      </div>

      <p className="text-sm text-text-faint">예약·티켓 내역 조회 기능은 준비 중이에요.</p>
    </div>
  )
}
