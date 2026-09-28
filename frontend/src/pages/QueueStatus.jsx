import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import Button from '../components/Button'
import { getQueuePosition } from '../api/reservations'
import { ApiError } from '../api/client'

const POLL_MS = 5000

export default function QueueStatus() {
  const { id } = useParams()
  const location = useLocation()
  const { sessionTitle, queuePosition: initialPosition, conferenceTitle, headcount, price } = location.state ?? {}
  const [position, setPosition] = useState(initialPosition ?? null)
  const [estimatedWaitMinutes, setEstimatedWaitMinutes] = useState(null)
  const [error, setError] = useState('')
  // 순번 조회가 404(대기열에 없음)면 대기가 끝난 것 — 자리가 나서 결제 단계로 올라갔거나 취소된 경우다
  const [leftQueue, setLeftQueue] = useState(false)

  useEffect(() => {
    let cancelled = false
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
          setLeftQueue(true)
          clearInterval(timer)
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
  }, [id])

  return (
    <div className="max-w-6xl mx-auto px-6 py-16 text-center">
      <h1 className="text-2xl font-semibold text-text mb-1">대기열 등록 완료</h1>
      {sessionTitle && <p className="text-text-muted mb-10">{sessionTitle}</p>}

      <div className="mx-auto w-52 h-52 rounded-full border-2 border-primary flex flex-col items-center justify-center mb-8">
        <span className="text-sm text-text-muted mb-1">내 순번</span>
        <span className="text-5xl font-bold text-primary">{position ?? '-'}</span>
      </div>

      <div className="max-w-sm mx-auto bg-surface border border-border rounded-xl px-5 py-4 mb-6 text-left">
        <div className="flex items-center justify-between">
          <span className="text-sm text-text-muted">세션</span>
          <span className="text-sm text-text">{sessionTitle ?? '-'}</span>
        </div>
        <div className="flex items-center justify-between mt-2">
          <span className="text-sm text-text-muted">결과 확인까지</span>
          <span className="text-sm text-text">
            {leftQueue ? '-' : estimatedWaitMinutes != null ? `늦어도 약 ${estimatedWaitMinutes}분` : '취소 발생 시'}
          </span>
        </div>
        <p className="text-xs text-text-faint mt-3 leading-relaxed">
          {leftQueue
            ? '대기가 끝났어요. 자리가 나서 결제 단계로 올라갔다면 마이페이지에서 결제를 진행해주세요.'
            : estimatedWaitMinutes != null
              ? '앞 순서의 결제 대기 좌석이 만료되면 자리가 나요. 그 전에 결제되면 계속 대기하게 돼요.'
              : '모든 좌석이 결제 완료된 상태예요. 취소가 생기면 순서대로 결제 안내를 드려요.'}
        </p>
      </div>

      {error && <p className="text-sm text-danger mb-4">{error}</p>}

      <p className="inline-flex items-center gap-2 text-xs text-success mb-8">
        <span className="w-1.5 h-1.5 rounded-full bg-success animate-pulse" /> 실시간 업데이트 중
      </p>

      <div className="flex items-center justify-center gap-3">
        <Link to="/conferences">
          <Button variant="secondary">목록으로</Button>
        </Link>
        {leftQueue ? (
          <Link to="/my">
            <Button>마이페이지로 가기</Button>
          </Link>
        ) : (
          <Link to={`/reservations/${id}/payment`} state={{ sessionTitle, conferenceTitle, headcount, price }}>
            <Button>결제하러 가기</Button>
          </Link>
        )}
      </div>
      <p className="text-xs text-text-faint mt-3">
        순번이 아직 도달하지 않았다면 결제 화면에서 안내해드려요
      </p>
    </div>
  )
}
