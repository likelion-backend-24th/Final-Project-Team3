import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { ChevronLeft, Plus, Pencil } from 'lucide-react'
import { getSessionsByConference } from '../../api/conferences'
import { ApiError } from '../../api/client'
import { formatDateRange } from '../../utils/date'
import Button from '../../components/Button'
import StatusBadge from '../../components/StatusBadge'

// 이제 소유자 스코프 API(GET /conferences/:id/sessions)가 생겨서 승인대기·반려 상태까지
// 전부 조회된다 — 예전엔 참가자용 공개 조회로 APPROVED만 볼 수 있었다.
export default function SessionManage() {
  const { id: conferenceId } = useParams()
  const location = useLocation()
  const [sessions, setSessions] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getSessionsByConference(conferenceId)
      .then((res) => setSessions(res.data))
      .catch((err) => setError(err instanceof ApiError ? err.message : '세션 목록을 불러오지 못했습니다.'))
  }, [conferenceId])

  const conferenceTitle = sessions?.[0]?.conferenceTitle

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <Link to="/organizer" className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text">
        <ChevronLeft size={16} /> 대시보드
      </Link>

      <div className="flex items-center justify-between mt-4 mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-text mb-1">세션 관리</h1>
          <p className="text-text-muted">{conferenceTitle ?? '컨퍼런스'}</p>
        </div>
        <Link to={`/organizer/conferences/${conferenceId}/sessions/new`}>
          <Button className="inline-flex items-center gap-1.5">
            <Plus size={16} /> 새 세션 등록
          </Button>
        </Link>
      </div>

      {location.state?.justCreatedSession && (
        <p className="mb-4 text-sm text-success bg-success/10 rounded-lg px-3 py-2">
          세션 등록 신청이 완료됐어요. 전체관리자 승인 후 참가자 화면에 표시됩니다.
        </p>
      )}
      {location.state?.justUpdatedSession && (
        <p className="mb-4 text-sm text-success bg-success/10 rounded-lg px-3 py-2">
          세션 수정이 반영됐어요. 수정한 세션은 다시 승인 대기 상태가 됩니다.
        </p>
      )}

      {error && <p className="text-sm text-danger mb-4">{error}</p>}

      <div className="space-y-3">
        {sessions?.map((s) => (
          <div key={s.id} className="bg-surface border border-border rounded-xl p-5">
            <div className="flex items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <p className="text-text font-medium">{s.title}</p>
                  <StatusBadge status={s.status} />
                </div>
                <p className="text-sm text-text-muted">
                  정원 {s.capacity}명 · 신청기간 {formatDateRange(s.startAt, s.endAt) ?? '-'}
                </p>
                {s.sessionStartAt && (
                  <p className="text-sm text-text-muted mt-0.5">
                    진행 {formatDateRange(s.sessionStartAt, s.sessionEndAt)}
                    {s.location && ` · ${s.location}`}
                    {s.speaker && ` · ${s.speaker}`}
                    {' · '}
                    {s.price > 0 ? `${s.price.toLocaleString()}원` : '무료'}
                  </p>
                )}
                {s.status === 'REJECTED' && s.rejectionReason && (
                  <p className="text-sm text-danger mt-1.5">반려 사유: {s.rejectionReason}</p>
                )}
              </div>
              <Link
                to={`/organizer/conferences/${conferenceId}/sessions/${s.id}/edit`}
                state={{ session: s }}
                className="shrink-0"
              >
                <Button variant="secondary" className="inline-flex items-center gap-1.5">
                  <Pencil size={14} /> 수정
                </Button>
              </Link>
            </div>
          </div>
        ))}
        {sessions?.length === 0 && (
          <p className="text-text-muted text-sm py-10 text-center">등록된 세션이 아직 없어요.</p>
        )}
      </div>
    </div>
  )
}
