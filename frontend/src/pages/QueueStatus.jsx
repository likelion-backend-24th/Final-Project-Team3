import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import Button from '../components/Button'
import { getQueuePosition } from '../api/reservations'

const POLL_MS = 5000

export default function QueueStatus() {
  const { id } = useParams()
  const location = useLocation()
  const { sessionTitle, queuePosition: initialPosition } = location.state ?? {}
  const [position, setPosition] = useState(initialPosition ?? null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    const poll = async () => {
      try {
        const res = await getQueuePosition(id)
        if (!cancelled) setPosition(res.data)
      } catch {
        if (!cancelled) setError('순번 조회에 실패했습니다.')
      }
    }
    poll()
    const timer = setInterval(poll, POLL_MS)
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
      </div>

      {error && <p className="text-sm text-danger mb-4">{error}</p>}

      <p className="inline-flex items-center gap-2 text-xs text-success mb-8">
        <span className="w-1.5 h-1.5 rounded-full bg-success animate-pulse" /> 실시간 업데이트 중
      </p>

      <div>
        <Link to="/conferences">
          <Button variant="secondary">목록으로</Button>
        </Link>
      </div>
    </div>
  )
}
