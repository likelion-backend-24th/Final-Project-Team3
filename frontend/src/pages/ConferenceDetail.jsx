import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Users, Calendar, MapPin, Mic } from 'lucide-react'
import { getConference } from '../api/conferences'
import { getCapacityStatus } from '../api/reservations'
import { formatDateRange } from '../utils/date'
import StatusBadge from '../components/StatusBadge'
import Button from '../components/Button'

export default function ConferenceDetail() {
  const { id } = useParams()
  const [conference, setConference] = useState(null)
  const [capacityBySession, setCapacityBySession] = useState({})
  const [error, setError] = useState('')

  useEffect(() => {
    getConference(id)
      .then((res) => {
        setConference(res.data)
        // 세션별 잔여좌석은 reservation-service에서 따로 관리해서, 목록엔 안 끼워주고
        // 화면에서 세션 수만큼 추가로 조회한다. 세션이 아주 많아지면 백엔드에 벌크 조회를
        // 요청하는 게 낫지만, 지금 규모(컨퍼런스당 세션 수)에선 괜찮다.
        Promise.allSettled((res.data.sessions ?? []).map((s) => getCapacityStatus(s.id))).then((results) => {
          const map = {}
          results.forEach((r, i) => {
            if (r.status === 'fulfilled') map[res.data.sessions[i].id] = r.value.data
          })
          setCapacityBySession(map)
        })
      })
      .catch(() => setError('컨퍼런스 정보를 불러오지 못했습니다.'))
  }, [id])

  if (error) return <p className="max-w-6xl mx-auto px-6 py-16 text-danger">{error}</p>
  if (!conference) return <p className="max-w-6xl mx-auto px-6 py-16 text-text-muted">불러오는 중...</p>

  const dateLabel = formatDateRange(conference.startAt, conference.endAt)

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <Link to="/conferences" className="text-sm text-text-muted hover:text-text">‹ 컨퍼런스 목록</Link>

      {conference.imageUrl && (
        <div
          className="mt-4 h-48 rounded-xl bg-cover bg-center"
          style={{ backgroundImage: `url(${conference.imageUrl})` }}
        />
      )}

      <div className="mt-4 mb-8">
        <div className="flex items-center gap-3 mb-1">
          <h1 className="text-2xl font-semibold text-text">{conference.title}</h1>
          <StatusBadge status={conference.status} />
        </div>
        {conference.organizerName && <p className="text-text-muted mb-3">{conference.organizerName}</p>}

        <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 text-sm text-text-muted">
          {dateLabel && (
            <span className="inline-flex items-center gap-1.5">
              <Calendar size={14} /> {dateLabel}
            </span>
          )}
          {conference.location && (
            <span className="inline-flex items-center gap-1.5">
              <MapPin size={14} /> {conference.location}
            </span>
          )}
          <span className="inline-flex items-center gap-1.5">
            <Users size={14} /> 정원 {conference.capacity}명
          </span>
        </div>

        {conference.tags?.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-3">
            {conference.tags.map((tag) => (
              <span
                key={tag}
                className="px-2 py-1 rounded-md text-xs text-text-muted bg-surface2 border border-border"
              >
                {tag}
              </span>
            ))}
          </div>
        )}

        {conference.description && (
          <p className="text-text-muted text-sm mt-4 whitespace-pre-wrap">{conference.description}</p>
        )}
      </div>

      <h2 className="text-sm font-medium text-text-muted mb-3">세션 목록</h2>
      <div className="space-y-3">
        {conference.sessions.map((s) => {
          const cap = capacityBySession[s.id]
          const soldOut = cap && cap.remaining <= 0
          return (
            <div
              key={s.id}
              className="bg-surface border border-border rounded-xl p-5 flex items-center justify-between gap-4"
            >
              <div>
                <p className="text-text font-medium mb-1">{s.title}</p>
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-text-muted">
                  {s.sessionStartAt && (
                    <span className="inline-flex items-center gap-1">
                      <Calendar size={13} /> {formatDateRange(s.sessionStartAt, s.sessionEndAt)}
                    </span>
                  )}
                  {s.location && (
                    <span className="inline-flex items-center gap-1">
                      <MapPin size={13} /> {s.location}
                    </span>
                  )}
                  {s.speaker && (
                    <span className="inline-flex items-center gap-1">
                      <Mic size={13} /> {s.speaker}
                    </span>
                  )}
                  <span>
                    {cap ? `잔여 ${Math.max(cap.remaining, 0)}/${cap.capacity}석` : `정원 ${s.capacity}명`}
                  </span>
                  <span className="font-medium text-text">{s.price > 0 ? `${s.price.toLocaleString()}원` : '무료'}</span>
                </div>
              </div>
              {soldOut ? (
                <Button disabled variant="secondary">마감</Button>
              ) : (
                <Link to={`/conferences/${conference.id}/sessions/${s.id}/apply`} state={{ session: s, conferenceTitle: conference.title }}>
                  <Button>신청하기</Button>
                </Link>
              )}
            </div>
          )
        })}
        {conference.sessions.length === 0 && (
          <p className="text-text-muted text-sm py-8 text-center">아직 등록된 세션이 없어요.</p>
        )}
      </div>
    </div>
  )
}
