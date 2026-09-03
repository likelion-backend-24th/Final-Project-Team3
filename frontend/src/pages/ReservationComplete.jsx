import { Link, useLocation, useParams } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'
import Button from '../components/Button'

export default function ReservationComplete() {
  const { id } = useParams()
  const location = useLocation()
  const { sessionTitle, conferenceTitle } = location.state ?? {}

  return (
    <div className="max-w-6xl mx-auto px-6 py-16 text-center">
      <CheckCircle2 className="mx-auto text-success mb-4" size={48} />
      <h1 className="text-2xl font-semibold text-text mb-1">신청이 완료됐습니다</h1>
      <p className="text-text-muted mb-8">정원 내 신청이 확정됐어요</p>

      <div className="max-w-sm mx-auto bg-surface border border-border rounded-xl overflow-hidden text-left">
        <div className="bg-primary/15 px-5 py-4">
          <p className="text-xs text-accent tracking-wide mb-1">TECHCONF RESERVATION</p>
          <p className="text-text font-medium">{sessionTitle ?? '세션'}</p>
          {conferenceTitle && <p className="text-sm text-text-muted">{conferenceTitle}</p>}
        </div>
        <div className="px-5 py-4 flex items-center justify-between">
          <span className="text-xs text-text-faint">예약 번호</span>
          <span className="text-sm text-text font-mono">{id}</span>
        </div>
      </div>

      <p className="text-xs text-text-faint mt-4">결제·QR 입장권 발급 기능은 준비 중이에요</p>

      <Link to="/my">
        <Button className="mt-8">마이페이지에서 확인</Button>
      </Link>
    </div>
  )
}
