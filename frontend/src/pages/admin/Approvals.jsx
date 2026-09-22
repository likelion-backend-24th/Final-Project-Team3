import { useEffect, useState } from 'react'
import { X, Paperclip } from 'lucide-react'
import {
  listPendingConferences,
  approveConference,
  rejectConference,
  listPendingSessions,
  approveSession,
  rejectSession,
  getConferenceDetail,
} from '../../api/admin'
import { downloadProofFile } from '../../api/conferences'
import { ApiError } from '../../api/client'
import { formatDateRange } from '../../utils/date'
import { saveBlob } from '../../utils/download'
import Button from '../../components/Button'
import StatusBadge from '../../components/StatusBadge'

// 승인/반려 결정 전에 세션·태그·장소까지 전부 보여주는 상세 모달.
function ConferenceDetailModal({ conferenceId, onClose }) {
  const [detail, setDetail] = useState(null)
  const [error, setError] = useState('')
  const [downloading, setDownloading] = useState(false)
  const [downloadError, setDownloadError] = useState('')

  const handleDownloadProof = async () => {
    setDownloadError('')
    setDownloading(true)
    try {
      const { blob, filename } = await downloadProofFile(conferenceId)
      saveBlob(blob, filename)
    } catch (err) {
      setDownloadError(err instanceof ApiError ? err.message : '증빙 파일을 내려받지 못했습니다.')
    } finally {
      setDownloading(false)
    }
  }

  useEffect(() => {
    let cancelled = false
    getConferenceDetail(conferenceId)
      .then((res) => {
        if (!cancelled) setDetail(res.data)
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError ? err.message : '상세 정보를 불러오지 못했습니다.')
      })
    return () => {
      cancelled = true
    }
  }, [conferenceId])

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div
        className="bg-surface border border-border rounded-xl p-6 max-w-2xl w-full max-h-[85vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-3 mb-4">
          <h2 className="text-lg font-semibold text-text">컨퍼런스 상세</h2>
          <button onClick={onClose} className="text-text-faint hover:text-text" aria-label="닫기">
            <X size={20} />
          </button>
        </div>

        {error && <p className="text-sm text-danger">{error}</p>}
        {!detail && !error && <p className="text-sm text-text-muted">불러오는 중...</p>}

        {detail && (
          <div className="space-y-5">
            {detail.imageUrl && (
              <img src={detail.imageUrl} alt={detail.title} className="w-full rounded-lg object-cover max-h-48" />
            )}

            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-xl font-semibold text-text">{detail.title}</h3>
                <StatusBadge status={detail.status} />
              </div>
              <p className="text-sm text-text-muted mt-1">
                {detail.organizerName} · {formatDateRange(detail.startAt, detail.endAt)}
              </p>
            </div>

            {detail.tags?.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {detail.tags.map((tag) => (
                  <span key={tag} className="text-xs bg-surface2 text-text-muted rounded-full px-2.5 py-1">
                    {tag}
                  </span>
                ))}
              </div>
            )}

            {detail.description && (
              <div>
                <h4 className="text-sm font-medium text-text mb-1">소개글</h4>
                <p className="text-sm text-text-muted whitespace-pre-wrap">{detail.description}</p>
              </div>
            )}

            <div>
              <h4 className="text-sm font-medium text-text mb-1">장소</h4>
              <p className="text-sm text-text-muted">{detail.location || '미입력'}</p>
              {detail.transportation && <p className="text-xs text-text-faint mt-0.5">교통편: {detail.transportation}</p>}
              {detail.parkingInfo && <p className="text-xs text-text-faint mt-0.5">주차: {detail.parkingInfo}</p>}
              {detail.amenities && <p className="text-xs text-text-faint mt-0.5">편의시설: {detail.amenities}</p>}
            </div>

            <div>
              <h4 className="text-sm font-medium text-text mb-1">증빙 파일</h4>
              {detail.proofFileAttached ? (
                <>
                  <button
                    type="button"
                    onClick={handleDownloadProof}
                    disabled={downloading}
                    className="inline-flex items-center gap-1.5 text-sm text-accent hover:underline disabled:opacity-50"
                  >
                    <Paperclip size={14} /> {downloading ? '내려받는 중...' : '증빙 파일 다운로드'}
                  </button>
                  {downloadError && <p className="text-xs text-danger mt-1">{downloadError}</p>}
                </>
              ) : (
                <p className="text-sm text-text-muted">첨부된 증빙 파일이 없어요.</p>
              )}
            </div>

            <div>
              <h4 className="text-sm font-medium text-text mb-2">세션 ({detail.sessions?.length ?? 0})</h4>
              {detail.sessions?.length > 0 ? (
                <div className="space-y-2">
                  {detail.sessions.map((s) => (
                    <div key={s.id} className="bg-surface2 rounded-lg p-3 flex items-center justify-between gap-3">
                      <div>
                        <p className="text-sm text-text">{s.title}</p>
                        <p className="text-xs text-text-faint">정원 {s.capacity}명 · {formatDateRange(s.startAt, s.endAt)}</p>
                      </div>
                      <StatusBadge status={s.status} />
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-text-muted">등록된 세션이 없어요.</p>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

// 세션은 이미 목록 조회 시점에 상세 필드를 전부 받아오므로 별도 API 호출 없이 그대로 보여준다.
function SessionDetailModal({ session, onClose }) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div
        className="bg-surface border border-border rounded-xl p-6 max-w-lg w-full max-h-[85vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-3 mb-4">
          <h2 className="text-lg font-semibold text-text">세션 상세</h2>
          <button onClick={onClose} className="text-text-faint hover:text-text" aria-label="닫기">
            <X size={20} />
          </button>
        </div>

        <div className="space-y-4">
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-xl font-semibold text-text">{session.title}</h3>
              <StatusBadge status={session.status} />
            </div>
            <p className="text-sm text-text-muted mt-1">{session.conferenceTitle ?? '소속 컨퍼런스 미상'}</p>
          </div>

          <div>
            <h4 className="text-sm font-medium text-text mb-1">신청 기간</h4>
            <p className="text-sm text-text-muted">{formatDateRange(session.startAt, session.endAt) || '미입력'}</p>
          </div>

          <div>
            <h4 className="text-sm font-medium text-text mb-1">진행 일시</h4>
            <p className="text-sm text-text-muted">
              {formatDateRange(session.sessionStartAt, session.sessionEndAt) || '미입력'}
            </p>
          </div>

          <div>
            <h4 className="text-sm font-medium text-text mb-1">정원 · 가격</h4>
            <p className="text-sm text-text-muted">
              정원 {session.capacity}명
              {session.maxHeadcountPerApplication ? ` · 1회 최대 ${session.maxHeadcountPerApplication}명 신청` : ''}
              {session.price ? ` · ${session.price.toLocaleString()}원` : ' · 무료'}
            </p>
          </div>

          {(session.location || session.speaker) && (
            <div>
              <h4 className="text-sm font-medium text-text mb-1">장소·발표자</h4>
              <p className="text-sm text-text-muted">{session.location || '미입력'}</p>
              {session.speaker && <p className="text-xs text-text-faint mt-0.5">발표자: {session.speaker}</p>}
            </div>
          )}

          {session.rejectionReason && (
            <div>
              <h4 className="text-sm font-medium text-text mb-1">반려 사유</h4>
              <p className="text-sm text-danger">{session.rejectionReason}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

// 반려 사유 입력 + 승인/반려 버튼을 공용으로 쓰는 한 줄 아이템.
function ApprovalItem({ title, subtitle, meta, proofAttached, onApprove, onReject, onViewDetail, busy }) {
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
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between sm:gap-3">
        <div className="min-w-0">
          <p className="text-text font-medium">{title}</p>
          {subtitle && <p className="text-sm text-text-muted mt-0.5">{subtitle}</p>}
          {meta && <p className="text-xs text-text-faint mt-1">{meta}</p>}
        </div>
        <div className="flex items-center gap-2 shrink-0">
          {proofAttached && (
            <span className="inline-flex items-center gap-1 text-xs text-text-muted whitespace-nowrap">
              <Paperclip size={12} /> 증빙
            </span>
          )}
          {onViewDetail && (
            <button onClick={onViewDetail} className="text-xs text-accent hover:underline">
              상세보기
            </button>
          )}
          <StatusBadge status="PENDING" />
        </div>
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
  const [viewingConferenceId, setViewingConferenceId] = useState(null)
  const [viewingSession, setViewingSession] = useState(null)

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
                subtitle={`${s.conferenceTitle ?? '소속 컨퍼런스 미상'} · 정원 ${s.capacity}명`}
                meta={formatDateRange(s.startAt, s.endAt)}
                busy={busyId === s.id}
                onApprove={() => approveSess(s.id)}
                onReject={(reason) => rejectSess(s.id, reason)}
                onViewDetail={() => setViewingSession(s)}
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
                proofAttached={c.proofFileAttached}
                busy={busyId === c.id}
                onApprove={() => approveConf(c.id)}
                onReject={(reason) => rejectConf(c.id, reason)}
                onViewDetail={() => setViewingConferenceId(c.id)}
              />
            ))}
          </div>
        )}
      </section>

      {viewingConferenceId && (
        <ConferenceDetailModal conferenceId={viewingConferenceId} onClose={() => setViewingConferenceId(null)} />
      )}
      {viewingSession && (
        <SessionDetailModal session={viewingSession} onClose={() => setViewingSession(null)} />
      )}
    </div>
  )
}
