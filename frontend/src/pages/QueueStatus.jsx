import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import Button from '../components/Button'
import { getQueuePosition, getMyReservations } from '../api/reservations'
import { ApiError } from '../api/client'
import { useAuth } from '../context/AuthContext'

const POLL_MS = 5000

// 대기 중(QUEUED)일 때는 순번만 보여주고 결제 버튼은 숨긴다. 앞에서 자리가 나면 서버가 이 예약을 결제 대기(HOLD)로
// 올리고 대기열에서 빼므로, 순번 조회가 404가 되면 내 예약 상태를 확인해서 "차례가 왔어요"로 바꿔 보여준다.
export default function QueueStatus() {
  const { id } = useParams()
  const location = useLocation()
  const { claims } = useAuth()
  const { sessionTitle, queuePosition: initialPosition, conferenceTitle, headcount, price } = location.state ?? {}
  const [position, setPosition] = useState(initialPosition ?? null)
  const [estimatedWaitMinutes, setEstimatedWaitMinutes] = useState(null)
  // null: 대기 중 / 'HOLD': 차례가 와서 결제 대기 / 'CONFIRMED' / 'CANCELLED' / 'UNKNOWN'
  const [outcome, setOutcome] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    const checkOutcome = async () => {
      try {
        const res = await getMyReservations(claims?.memberId)
        const mine = (res.data ?? []).find((r) => r.reservationId === id)
        if (!cancelled) setOutcome(mine?.status ?? 'UNKNOWN')
      } catch {
        if (!cancelled) setOutcome('UNKNOWN')
      }
    }
    const poll = async () => {
      try {
        const res = await getQueuePosition(id)
        if (cancelled) return
        // estimatedWaitMinutes: 결제 대기 좌석이 만료되는 시각 기준 "늦어도 N분 안에 결과".
        // null이면 좌석이 모두 결제 완료돼서 취소가 나와야만 자리가 난다(시간 예측 불가).
        setPosition(res.data.position)
        setEstimatedWaitMinutes(res.data.estimatedWaitMinutes)
        setError('')
      } catch (err) {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 404) {
          clearInterval(timer)
          checkOutcome()
        } else {
          setError('순번 조회에 실패했습니다.')
        }
      }
    }
    const timer = setInterval(poll, POLL_MS)
    poll()
    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [id, claims?.memberId])

  const myTurn = outcome === 'HOLD'
  const waiting = outcome === null

  return (
    <div className="max-w-6xl mx-auto px-6 py-16 text-center">
      <h1 className="text-2xl font-semibold text-text mb-1">
        {myTurn ? '차례가 왔어요!' : waiting ? '대기열 등록 완료' : '대기가 끝났어요'}
      </h1>
      {sessionTitle && <p className="text-text-muted mb-10">{sessionTitle}</p>}

      {waiting ? (
        <div className="mx-auto w-52 h-52 rounded-full border-2 border-primary flex flex-col items-center justify-center mb-8">
          <span className="text-sm text-text-muted mb-1">내 순번</span>
          <span className="text-5xl font-bold text-primary">{position ?? '-'}</span>
        </div>
      ) : (
        <p className="max-w-sm mx-auto text-text mb-8 leading-relaxed">
          {myTurn && '자리가 나서 결제 단계로 올라갔어요. 10분 안에 결제하지 않으면 다음 대기자에게 넘어가요.'}
          {outcome === 'CONFIRMED' && '이미 결제가 끝난 예약이에요. 마이페이지에서 QR 티켓을 확인하세요.'}
          {outcome === 'CANCELLED' && '대기가 취소됐어요. 다시 신청하려면 세션 목록에서 신청해주세요.'}
          {outcome === 'UNKNOWN' && '예약 상태는 마이페이지에서 확인할 수 있어요.'}
        </p>
      )}

      {waiting && (
        <div className="max-w-sm mx-auto bg-surface border border-border rounded-xl px-5 py-4 mb-6 text-left">
          <div className="flex items-center justify-between">
            <span className="text-sm text-text-muted">세션</span>
            <span className="text-sm text-text">{sessionTitle ?? '-'}</span>
          </div>
          <div className="flex items-center justify-between mt-2">
            <span className="text-sm text-text-muted">결과 확인까지</span>
            <span className="text-sm text-text">
              {estimatedWaitMinutes != null ? `늦어도 약 ${estimatedWaitMinutes}분` : '취소 발생 시'}
            </span>
          </div>
          <p className="text-xs text-text-faint mt-3 leading-relaxed">
            {estimatedWaitMinutes != null
              ? '앞 순서의 결제 대기 좌석이 만료되면 자리가 나요. 그 전에 결제되면 계속 대기하게 돼요.'
              : '모든 좌석이 결제 완료된 상태예요. 취소가 생기면 순서대로 결제 안내를 드려요.'}
          </p>
        </div>
      )}

      {error && <p className="text-sm text-danger mb-4">{error}</p>}

      {waiting && (
        <p className="inline-flex items-center gap-2 text-xs text-success mb-8">
          <span className="w-1.5 h-1.5 rounded-full bg-success animate-pulse" /> 실시간 업데이트 중 · 차례가 오면 이 화면에서 바로 알려드려요
        </p>
      )}

      <div className="flex items-center justify-center gap-3">
        <Link to="/conferences">
          <Button variant="secondary">목록으로</Button>
        </Link>
        {myTurn ? (
          <Link to={`/reservations/${id}/payment`} state={{ sessionTitle, conferenceTitle, headcount, price }}>
            <Button>결제하러 가기</Button>
          </Link>
        ) : (
          <Link to="/my">
            <Button variant={waiting ? 'secondary' : 'primary'}>마이페이지</Button>
          </Link>
        )}
      </div>
    </div>
  )
}
