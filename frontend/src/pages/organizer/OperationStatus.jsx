import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronLeft, RefreshCw } from 'lucide-react'
import { getOperationStatus } from '../../api/conferences'
import { ApiError } from '../../api/client'

function StatCell({ label, value, className = '' }) {
  return (
    <div className={`text-center ${className}`}>
      <p className="text-lg font-semibold text-text">{value}</p>
      <p className="text-xs text-text-faint mt-0.5">{label}</p>
    </div>
  )
}

export default function OperationStatus() {
  const { id: conferenceId } = useParams()
  const [status, setStatus] = useState(null)
  const [error, setError] = useState('')
  const [refreshing, setRefreshing] = useState(false)

  const load = useCallback(
    (isRefresh) => {
      if (isRefresh) setRefreshing(true)
      setError('')
      getOperationStatus(conferenceId)
        .then((res) => setStatus(res.data))
        .catch((err) => setError(err instanceof ApiError ? err.message : '운영 현황을 불러오지 못했습니다.'))
        .finally(() => setRefreshing(false))
    },
    [conferenceId],
  )

  useEffect(() => load(false), [load])

  return (
    <div className="max-w-4xl mx-auto px-6 py-10">
      <Link to="/organizer" className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text">
        <ChevronLeft size={16} /> 대시보드
      </Link>

      <div className="flex items-start justify-between gap-4 mt-4 mb-1">
        <h1 className="text-2xl font-semibold text-text">운영 현황</h1>
        <button
          onClick={() => load(true)}
          disabled={refreshing}
          className="inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text disabled:opacity-50 shrink-0 mt-1"
        >
          <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} /> 새로고침
        </button>
      </div>
      <p className="text-text-muted mb-8">세션별 신청·입장 현황</p>

      {error && <p className="text-danger">{error}</p>}
      {!status && !error && <p className="text-text-muted">불러오는 중...</p>}

      {status && (
        <div className="space-y-3">
          {status.sessions.map((s) => (
            <div key={s.sessionId} className="bg-surface border border-border rounded-xl p-5">
              <p className="text-text font-medium mb-4">{s.title}</p>
              <div className="grid grid-cols-5 gap-2">
                <StatCell label="홀드" value={s.holdCount} />
                <StatCell label="대기열" value={s.queuedCount} />
                <StatCell label="확정" value={s.confirmedCount} className="text-success" />
                <StatCell label="취소" value={s.cancelledCount} className="text-danger" />
                <StatCell label="체크인" value={s.checkedInCount} className="text-primary" />
              </div>
            </div>
          ))}
          {status.sessions.length === 0 && (
            <p className="text-text-muted text-sm text-center py-16">등록된 세션이 없어요.</p>
          )}
        </div>
      )}
    </div>
  )
}
