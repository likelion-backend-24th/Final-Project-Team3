import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { Lock, CreditCard, Wallet, Landmark, Smartphone, ShieldCheck } from 'lucide-react'
import Button from '../components/Button'
import { submitPayment } from '../api/reservations'
import { ApiError } from '../api/client'

// 포트원(PortOne) 실제 연동 전까지의 자리 표시 UI다 — 실제 붙이면 이 목록이 아니라 포트원
// SDK가 띄우는 결제창으로 대체되고, 카드번호 같은 민감정보는 우리 페이지에 절대 두면 안 된다.
const METHODS = [
  { id: 'CARD', label: '카드결제', icon: CreditCard },
  { id: 'KAKAOPAY', label: '카카오페이', icon: Wallet },
  { id: 'NAVERPAY', label: '네이버페이', icon: Wallet },
  { id: 'TRANSFER', label: '계좌이체', icon: Landmark },
  { id: 'PHONE', label: '휴대폰 소액결제', icon: Smartphone },
]

// Session에 가격 필드가 없어 모든 세션은 사실상 무료다 — 결제는 PG 없는 Mock이라
// paymentMethod만 의미가 있고 amount는 항상 0으로 보낸다.
export default function Payment() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { sessionTitle, conferenceTitle, headcount } = location.state ?? {}

  const [method, setMethod] = useState('CARD')
  const [error, setError] = useState('')
  const [queueBlocked, setQueueBlocked] = useState(false)
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setQueueBlocked(false)
    setLoading(true)
    try {
      await submitPayment(id, { paymentMethod: method, amount: 0 })
      navigate(`/reservations/${id}/complete`, { state: { sessionTitle, conferenceTitle } })
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        setQueueBlocked(true)
      } else {
        setError(err instanceof ApiError ? err.message : '결제에 실패했습니다.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <div className="max-w-xl mx-auto">
        <h1 className="text-2xl font-semibold text-text mb-4">결제</h1>

        <div className="bg-primary/10 border border-primary/30 rounded-xl p-4 mb-5 flex gap-3">
          <Lock size={18} className="text-primary shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-primary">좌석 홀드 완료 — 결제를 완료해 주세요</p>
            <p className="text-xs text-text-muted mt-0.5">
              좌석이 임시 확보됐습니다. 결제 완료 전까지는 좌석이 확정되지 않으며, 결제 실패 시 홀드가 자동 해제됩니다.
            </p>
          </div>
        </div>

        {queueBlocked && (
          <div className="bg-warning/10 border border-warning/30 rounded-xl p-4 mb-5 text-sm text-warning">
            아직 결제 순서가 되지 않았어요. 대기열에서 순서를 기다려주세요.{' '}
            <Link to={`/reservations/${id}/queue`} className="underline">대기열 화면으로</Link>
          </div>
        )}

        <div className="bg-surface border border-border rounded-xl p-5 mb-5">
          <p className="text-sm font-medium text-text-muted mb-3">주문 내역</p>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-text">{sessionTitle ?? '세션'}</span>
              <span className="text-text-muted">{headcount ?? 1}인</span>
            </div>
            {conferenceTitle && (
              <div className="flex justify-between text-text-muted">
                <span>컨퍼런스</span>
                <span>{conferenceTitle}</span>
              </div>
            )}
            <div className="flex justify-between text-text-muted">
              <span>단가</span>
              <span>무료</span>
            </div>
          </div>
          <div className="flex justify-between items-center border-t border-border mt-4 pt-4">
            <span className="text-text font-medium">합계</span>
            <span className="text-lg font-semibold text-primary">0원</span>
          </div>
        </div>

        <form onSubmit={submit} className="bg-surface border border-border rounded-xl p-5">
          <p className="text-sm font-medium text-text mb-3">결제 수단</p>
          <div className="space-y-2 mb-4">
            {METHODS.map((m) => {
              const Icon = m.icon
              const active = method === m.id
              return (
                <button
                  key={m.id}
                  type="button"
                  onClick={() => setMethod(m.id)}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg border text-sm font-medium transition-colors ${
                    active
                      ? 'bg-surface2 border-primary text-text'
                      : 'bg-transparent border-border text-text-muted hover:text-text'
                  }`}
                >
                  <Icon size={18} className={active ? 'text-primary' : 'text-text-faint'} />
                  {m.label}
                  <span
                    className={`ml-auto w-4 h-4 rounded-full border-2 ${
                      active ? 'border-primary bg-primary' : 'border-border'
                    }`}
                  />
                </button>
              )
            })}
          </div>

          {error && <p className="text-sm text-danger mt-2">{error}</p>}

          <Button type="submit" loading={loading} className="w-full mt-2">
            0원 결제하기
          </Button>
          <p className="flex items-center justify-center gap-1.5 text-xs text-text-faint mt-3">
            <ShieldCheck size={13} /> 포트원(PortOne)으로 안전하게 결제돼요 · 결제 완료 즉시 QR 티켓이 발급됩니다
          </p>
        </form>
      </div>
    </div>
  )
}
