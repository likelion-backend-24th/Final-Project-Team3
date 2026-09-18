import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import * as PortOne from '@portone/browser-sdk/v2'
import { Lock, ShieldCheck } from 'lucide-react'
import Button from '../components/Button'
import { getPgConfig, submitPayment } from '../api/reservations'
import { ApiError } from '../api/client'

// PortOne 결제창 안에서 결제수단을 고른다. payMethod는 결제창을 열 때 먼저 지정해야 해서
// ponytail: 카드결제로 고정한다. 카카오페이 등 간편결제는 PortOne의 EASY_PAY
// 서브필드 구조가 따로 있어서, 필요해지면 그때 선택 UI와 함께 추가한다.
const PAY_METHOD = 'CARD'

export default function Payment() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { sessionTitle, conferenceTitle, headcount = 1, price = 0 } = location.state ?? {}
  const amount = price * headcount

  const [error, setError] = useState('')
  const [queueBlocked, setQueueBlocked] = useState(false)
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setQueueBlocked(false)
    setLoading(true)
    try {
      let paymentId = id
      if (amount > 0) {
        const { storeId, channelKey } = (await getPgConfig()).data
        const response = await PortOne.requestPayment({
          storeId,
          channelKey,
          paymentId: id,
          orderName: sessionTitle ?? '세션 신청',
          totalAmount: amount,
          currency: 'CURRENCY_KRW',
          payMethod: PAY_METHOD,
        })
        if (response.code !== undefined) {
          setError(response.message ?? '결제가 취소되었습니다.')
          setLoading(false)
          return
        }
        paymentId = response.paymentId
      }

      await submitPayment(id, { paymentId })
      navigate(`/reservations/${id}/complete`, { state: { sessionTitle, conferenceTitle } })
    } catch (err) {
      // 403(아직 내 순번 아님)과 409 RESERVATION_SESSION_CAPACITY_EXCEEDED(순번은 됐지만 그 사이
      // 다른 사람이 자리를 채워서 아직 빈 자리가 없음)는 참가자 입장에서 결국 같은 상황이다 —
      // "지금은 결제 못 하니 대기열에서 좀 더 기다려야 함". 그래서 같이 취급한다.
      const isQueueBlocked =
        err instanceof ApiError && (err.status === 403 || err.code === 'RESERVATION_SESSION_CAPACITY_EXCEEDED')
      if (isQueueBlocked) {
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
            아직 결제할 수 없어요. 대기열 순번이 됐어도 그 사이 자리가 다시 찰 수 있어요 — 잠시 후 대기열 화면에서 다시 시도해주세요.{' '}
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
              <span>{price > 0 ? `${price.toLocaleString()}원` : '무료'}</span>
            </div>
          </div>
          <div className="flex justify-between items-center border-t border-border mt-4 pt-4">
            <span className="text-text font-medium">합계</span>
            <span className="text-lg font-semibold text-primary">{amount > 0 ? `${amount.toLocaleString()}원` : '0원'}</span>
          </div>
        </div>

        <form onSubmit={submit} className="bg-surface border border-border rounded-xl p-5">
          {error && <p className="text-sm text-danger mb-2">{error}</p>}

          <Button type="submit" loading={loading} className="w-full">
            {amount > 0 ? `${amount.toLocaleString()}원 결제하기` : '무료 신청 완료하기'}
          </Button>
          {amount > 0 && (
            <p className="flex items-center justify-center gap-1.5 text-xs text-text-faint mt-3">
              <ShieldCheck size={13} /> 포트원(PortOne) 결제창에서 안전하게 결제돼요 · 결제 완료 즉시 QR 티켓이 발급됩니다
            </p>
          )}
        </form>
      </div>
    </div>
  )
}
