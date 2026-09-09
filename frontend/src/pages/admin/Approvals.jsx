import { useEffect, useState } from 'react'
import {
  listPendingConferences,
  approveConference,
  rejectConference,
  listPendingSessions,
  approveSession,
  rejectSession,
} from '../../api/admin'
import { ApiError } from '../../api/client'
import { formatDateRange } from '../../utils/date'
import Button from '../../components/Button'
import StatusBadge from '../../components/StatusBadge'

// 반려 사유 입력 + 승인/반려 버튼을 공용으로 쓰는 한 줄 아이템.
function ApprovalItem({ title, subtitle, meta, onApprove, onReject, busy }) {
  const [rejecting, setRejecting] = useState(false)
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')

  const submitReject = async () => {
    if (!reason.trim()) {
      setError('반려 사유를 입력해주세요.')
      return
    }
    setError('')
    await onReject(reason.trim())
  }

  return (
    <div className="bg-surface border border-border rounded-xl p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-text font-medium">{title}</p>
          {subtitle && <p className="text-sm text-text-muted mt-0.5">{subtitle}</p>}
          {meta && <p className="text-xs text-text-faint mt-1">{meta}</p>}
        </div>
        <StatusBadge status="PENDING" />
      </div>

      {rejecting ? (
        <div className="mt-4 space-y-2">
          <textarea
            className="w-full bg-bg border border-border rounded-lg px-4 py-3 text-sm text-text placeholder:text-text-faint focus:outline-none focus:border-primary"
            rows={2}
            placeholder="반려 사유를 입력하세요"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
          {error && <p className="text-xs text-danger">{error}</p>}
          <div className="flex gap-2">
            <Button variant="secondary" onClick={() => setRejecting(false)} disabled={busy}>
              취소
            </Button>
            <Button variant="danger" onClick={submitReject} loading={busy}>
              반려 확정
            </Button>
          </div>
        </div>
      ) : (
        <div className="flex gap-2 mt-4">
          <Button onClick={onApprove} loading={busy}>
            승인
          </Button>
          <Button variant="danger" onClick={() => setRejecting(true)} disabled={busy}>
            반려
          </Button>
        </div>
      )}
    </div>
  )
}

export default function Approvals() {
  const [conferences, setConferences] = useState([])
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  useEffect(() => {
    let cancelled = false
    Promise.all([listPendingConferences(), listPendingSessions()])
      .then(([confRes, sessionRes]) => {
        if (cancelled) return
        setConferences(confRes.data ?? [])
        setSessions(sessionRes.data ?? [])
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError ? err.message : '승인 대기 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const handle = async (id, action) => {
    setBusyId(id)
    try {
      await action()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '처리에 실패했습니다.')
    } finally {
      setBusyId(null)
    }
  }

  const approveConf = (id) =>
    handle(id, async () => {
      await approveConference(id)
      setConferences((list) => list.filter((c) => c.id !== id))
    })
  const rejectConf = (id, reason) =>
    handle(id, async () => {
      await rejectConference(id, reason)
      setConferences((list) => list.filter((c) => c.id !== id))
    })
  const approveSess = (id) =>
    handle(id, async () => {
      await approveSession(id)
      setSessions((list) => list.filter((s) => s.id !== id))
    })
  const rejectSess = (id, reason) =>
    handle(id, async () => {
      await rejectSession(id, reason)
      setSessions((list) => list.filter((s) => s.id !== id))
    })

  if (loading) {
    return <div className="max-w-6xl mx-auto px-6 py-20 text-center text-text-muted">불러오는 중...</div>
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-semibold text-text mb-1">승인 관리</h1>
      <p className="text-text-muted mb-8">세션 · 컨퍼런스 승인</p>

      {error && <p className="text-sm text-danger mb-4">{error}</p>}

      <section className="mb-10">
        <div className="flex items-center gap-2 mb-4">
          <h2 className="text-lg font-semibold text-text">세션 승인 대기</h2>
          <span className="text-xs bg-warning/10 text-warning rounded-full px-2 py-0.5">{sessions.length}</span>
        </div>
        {sessions.length === 0 ? (
          <p className="text-sm text-text-muted">승인 대기 중인 세션이 없어요.</p>
        ) : (
          <div className="space-y-4">
            {sessions.map((s) => (
              <ApprovalItem
                key={s.id}
                title={s.title}
                subtitle={`정원 ${s.capacity}명`}
                meta={formatDateRange(s.startAt, s.endAt)}
                busy={busyId === s.id}
                onApprove={() => approveSess(s.id)}
                onReject={(reason) => rejectSess(s.id, reason)}
              />
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 className="text-lg font-semibold text-text mb-4">승인 대기 컨퍼런스</h2>
        {conferences.length === 0 ? (
          <p className="text-sm text-text-muted">승인 대기 중인 컨퍼런스가 없어요.</p>
        ) : (
          <div className="space-y-4">
            {conferences.map((c) => (
              <ApprovalItem
                key={c.id}
                title={c.title}
                subtitle={`${c.organizerName ?? '주최자 미상'} · ${formatDateRange(c.startAt, c.endAt) ?? '-'} · ${c.location ?? '-'}`}
                meta={c.description}
                busy={busyId === c.id}
                onApprove={() => approveConf(c.id)}
                onReject={(reason) => rejectConf(c.id, reason)}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
