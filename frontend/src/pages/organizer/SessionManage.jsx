import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { ChevronLeft, Plus, Info } from 'lucide-react'
import { getConference } from '../../api/conferences'
import { ApiError } from '../../api/client'
import { formatDateRange } from '../../utils/date'
import Button from '../../components/Button'
import StatusBadge from '../../components/StatusBadge'

// 주최자 소유 스코프로 "내 세션 전체(승인대기·반려 포함)"를 조회하는 API가 아직 없어서,
// 참가자용 공개 조회(getConference)로 승인된 세션만 우선 보여준다. 백엔드에 전용 API가
// 생기면 이 fetch만 교체하면 된다.
export default function SessionManage() {
  const { id: conferenceId } = useParams()
  const location = useLocation()
  const [conference, setConference] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getConference(conferenceId)
      .then((res) => setConference(res.data))
      .catch((err) => setError(err instanceof ApiError ? err.message : '컨퍼런스 정보를 불러오지 못했습니다.'))
  }, [conferenceId])

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <Link to="/organizer" className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text">
        <ChevronLeft size={16} /> 대시보드
      </Link>

      <div className="flex items-center justify-between mt-4 mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-text mb-1">세션 관리</h1>
          <p className="text-text-muted">{conference?.title ?? '컨퍼런스'}</p>
        </div>
        <Link to={`/organizer/conferences/${conferenceId}/sessions/new`}>
          <Button className="inline-flex items-center gap-1.5">
            <Plus size={16} /> 새 세션 등록
          </Button>
        </Link>
      </div>

      {location.state?.justCreatedSession && (
        <p className="mb-4 text-sm text-success bg-success/10 rounded-lg px-3 py-2">
          세션 등록 신청이 완료됐어요. 전체관리자 승인 후 아래 목록·참가자 화면에 표시됩니다.
        </p>
      )}

      <div className="flex gap-2 bg-warning/10 text-warning text-sm rounded-lg px-3 py-2.5 mb-6">
        <Info size={16} className="shrink-0 mt-0.5" />
        <span>
          이 목록엔 승인된 세션만 표시돼요. 주최자가 승인 대기·반려 상태까지 포함해 조회하는 API가 아직 없어서,
          방금 등록한 세션은 전체관리자 승인 전까지 여기 안 보일 수 있어요.
        </span>
      </div>

      {error && <p className="text-sm text-danger mb-4">{error}</p>}

      <div className="space-y-3">
        {conference?.sessions?.map((s) => (
          <div key={s.id} className="bg-surface border border-border rounded-xl p-5 flex items-center justify-between">
            <div>
              <p className="text-text font-medium">{s.title}</p>
              <p className="text-sm text-text-muted mt-1">
                정원 {s.capacity}명 · 신청기간 {formatDateRange(s.startAt, s.endAt) ?? '-'}
              </p>
            </div>
            <StatusBadge status="APPROVED" />
          </div>
        ))}
        {conference && conference.sessions?.length === 0 && (
          <p className="text-text-muted text-sm py-10 text-center">승인된 세션이 아직 없어요.</p>
        )}
      </div>
    </div>
  )
}
