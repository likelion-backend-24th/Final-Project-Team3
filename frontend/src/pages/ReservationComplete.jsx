import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { QRCodeSVG } from 'qrcode.react'
import { CheckCircle2 } from 'lucide-react'
import Button from '../components/Button'
import { getQrTickets } from '../api/reservations'
import { ApiError } from '../api/client'

export default function ReservationComplete() {
  const { id } = useParams()
  const location = useLocation()
  const { sessionTitle, conferenceTitle } = location.state ?? {}

  const [ticket, setTicket] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    getQrTickets(id)
      .then((res) => {
        if (!cancelled) setTicket(res.data?.[0] ?? null)
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError ? err.message : 'QR 티켓 조회에 실패했습니다.')
      })
    return () => {
      cancelled = true
    }
  }, [id])

  return (
    <div className="max-w-6xl mx-auto px-6 py-16 text-center">
      <CheckCircle2 className="mx-auto text-success mb-4" size={48} />
      <h1 className="text-2xl font-semibold text-text mb-1">예약 완료! 티켓이 발급됐습니다</h1>
      <p className="text-text-muted mb-8">마이페이지에서 언제든 확인할 수 있어요</p>

      <div className="max-w-sm mx-auto bg-white rounded-xl overflow-hidden text-left">
        <div className="bg-primary px-5 py-4">
          <p className="text-xs text-white/70 tracking-wide mb-1">TECHCONF TICKET</p>
          <p className="text-white font-medium">{sessionTitle ?? '세션'}</p>
          {conferenceTitle && <p className="text-sm text-white/80">{conferenceTitle}</p>}
        </div>
        <div className="px-5 py-4 flex items-center justify-between gap-4">
          <div>
            <p className="text-xs text-gray-400 mb-1">결제 금액</p>
            <p className="text-sm text-gray-800">무료</p>
          </div>
          {ticket ? (
            <QRCodeSVG value={ticket.code} size={96} />
          ) : (
            <div className="w-24 h-24 rounded-lg bg-gray-100 animate-pulse" />
          )}
        </div>
        <div className="border-t border-dashed border-gray-200 px-5 py-3 flex items-center justify-between">
          <span className="text-xs text-gray-500 font-mono">{ticket?.code ?? id}</span>
          <span className="text-xs bg-gray-100 text-gray-600 rounded-md px-2 py-1">입장권</span>
        </div>
      </div>

      {error && <p className="text-sm text-danger mt-4">{error}</p>}

      <Link to="/my">
        <Button className="mt-8">마이페이지에서 확인</Button>
      </Link>
    </div>
  )
}
