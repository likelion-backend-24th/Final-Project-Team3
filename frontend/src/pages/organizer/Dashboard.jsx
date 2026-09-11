import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { getMyConferences, getSessionsByConference } from '../../api/conferences'
import { getCapacityStatus } from '../../api/reservations'
import { formatDateRange } from '../../utils/date'
import StatusBadge from '../../components/StatusBadge'
import Button from '../../components/Button'

export default function Dashboard() {
  const location = useLocation()
  const [conferences, setConferences] = useState(null)
  const [sessionsByConference, setSessionsByConference] = useState({})
  const [capacityBySession, setCapacityBySession] = useState({})
  const [error, setError] = useState('')

  useEffect(() => {
    getMyConferences()
      .then((res) => {
        setConferences(res.data)
        // 세션 목록은 승인된 컨퍼런스에서만 의미가 있다(미승인 컨퍼런스엔 세션 자체를 못 만듦).
        const approved = res.data.filter((c) => c.status === 'APPROVED')
        return Promise.allSettled(
          approved.map((c) => getSessionsByConference(c.id).then((r) => [c.id, r.data])),
        )
      })
      .then((results) => {
        const byConference = {}
        const allSessions = []
        results.forEach((r) => {
          if (r.status !== 'fulfilled') return
          const [conferenceId, sessions] = r.value
          byConference[conferenceId] = sessions
          allSessions.push(...sessions)
        })
        setSessionsByConference(byConference)

        // 잔여좌석·확정인원은 conference-service가 아니라 reservation-service가 실데이터를 갖고 있어서
        // (ConferenceDetail.jsx와 동일 패턴) 세션마다 따로 물어봐야 한다.
        return Promise.allSettled(allSessions.map((s) => getCapacityStatus(s.id).then((r) => [s.id, r.data])))
      })
      .then((results) => {
        const byCession = {}
        results.forEach((r) => {
          if (r.status !== 'fulfilled') return
          const [sessionId, data] = r.value
          byCession[sessionId] = data
        })
        setCapacityBySession(byCession)
      })
      .catch(() => setError('컨퍼런스 목록을 불러오지 못했습니다.'))
  }, [])

  const pending = conferences?.filter((c) => c.status === 'PENDING').length ?? 0
  const approved = conferences?.filter((c) => c.status === 'APPROVED').length ?? 0

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-semibold text-text mb-1">주최자 대시보드</h1>
          <p className="text-text-muted">주최한 컨퍼런스와 운영 현황</p>
        </div>
        <Link to="/organizer/conferences/new">
          <Button className="inline-flex items-center gap-1.5">
            <Plus size={16} /> 컨퍼런스 등록
          </Button>
        </Link>
      </div>

      {location.state?.justCreated && (
        <p className="mb-6 text-sm text-success bg-success/10 rounded-lg px-3 py-2">
          컨퍼런스 등록 신청이 완료됐어요. 전체관리자 승인 후 아래 목록에 표시됩니다.
        </p>
      )}

      <div className="grid grid-cols-3 gap-4 mb-10">
        <div className="bg-surface border border-border rounded-xl p-5">
          <p className="text-sm text-text-muted mb-1">전체 컨퍼런스</p>
          <p className="text-2xl font-semibold text-text">{conferences?.length ?? '-'}</p>
        </div>
        <div className="bg-surface border border-border rounded-xl p-5">
          <p className="text-sm text-text-muted mb-1">승인 대기</p>
          <p className="text-2xl font-semibold text-warning">{pending}</p>
        </div>
        <div className="bg-surface border border-border rounded-xl p-5">
          <p className="text-sm text-text-muted mb-1">승인됨</p>
          <p className="text-2xl font-semibold text-success">{approved}</p>
        </div>
      </div>

      <h2 className="text-sm font-medium text-text-muted mb-3">내 컨퍼런스</h2>
      {error && <p className="text-danger">{error}</p>}
      <div className="space-y-3">
        {conferences?.map((c) => (
          <div key={c.id} className="bg-surface border border-border rounded-xl p-5">
            <div className="flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <p className="text-text font-medium">{c.title}</p>
                  <StatusBadge status={c.status} />
                </div>
                <p className="text-sm text-text-muted">
                  {formatDateRange(c.startAt, c.endAt)} · 정원 {c.capacity}명
                </p>
              </div>
              {c.status === 'APPROVED' && (
                <div className="flex gap-2">
                  <Link to="/organizer/operations">
                    <Button variant="secondary">운영 현황</Button>
                  </Link>
                  <Link to={`/organizer/conferences/${c.id}/sessions`}>
                    <Button variant="secondary">세션 설정</Button>
                  </Link>
                  <Link to={`/organizer/conferences/${c.id}/settings`}>
                    <Button variant="secondary">설정</Button>
                  </Link>
                </div>
              )}
            </div>

            {c.status === 'PENDING' && (
              <p className="text-sm text-warning mt-3">
                전체관리자 검토 중입니다. 승인 후 세션 설정 및 운영이 가능합니다.
              </p>
            )}

            {c.status === 'APPROVED' && sessionsByConference[c.id]?.length > 0 && (
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-4">
                {sessionsByConference[c.id].map((s) => (
                  <SessionProgress key={s.id} session={s} capacity={capacityBySession[s.id]} />
                ))}
              </div>
            )}
          </div>
        ))}
        {conferences?.length === 0 && (
          <p className="text-text-muted text-sm py-10 text-center">등록한 컨퍼런스가 아직 없어요.</p>
        )}
      </div>
    </div>
  )
}

// 잔여좌석 기준 진행률 바. 정원이 꽉 차면 "마감"으로 표시한다 — 대기 인원수를 주는 API가
// 아직 없어서(WaitingQueueRepository엔 값이 있지만 컨트롤러에 안 뚫려있음) 정확한 "대기 N명"은 못 보여준다.
function SessionProgress({ session, capacity }) {
  if (!capacity) {
    return (
      <div className="bg-bg border border-border rounded-lg p-3">
        <p className="text-sm text-text font-medium truncate mb-1.5">{session.title}</p>
        <p className="text-xs text-text-faint">정원 {session.capacity}명</p>
      </div>
    )
  }

  const soldOut = capacity.remaining <= 0
  const percent = Math.min(100, Math.round((capacity.confirmedCount / capacity.capacity) * 100))
  const barColor = soldOut ? 'bg-danger' : percent >= 90 ? 'bg-warning' : 'bg-success'

  return (
    <div className="bg-bg border border-border rounded-lg p-3">
      <p className="text-sm text-text font-medium truncate mb-1.5">{session.title}</p>
      <div className="flex items-center justify-between text-xs mb-1.5">
        {soldOut ? (
          <span className="text-danger font-medium">마감</span>
        ) : (
          <span className="text-text-muted">잔여 {capacity.remaining}석</span>
        )}
        {!soldOut && <span className="text-text-faint">{percent}%</span>}
      </div>
      <div className="h-1.5 rounded-full bg-surface2 overflow-hidden">
        <div className={`h-full rounded-full ${barColor}`} style={{ width: `${soldOut ? 100 : percent}%` }} />
      </div>
    </div>
  )
}
