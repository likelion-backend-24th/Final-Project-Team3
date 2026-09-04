import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Users, Calendar, MapPin } from 'lucide-react'
import { getConference } from '../api/conferences'
import { formatDateRange } from '../utils/date'
import StatusBadge from '../components/StatusBadge'
import Button from '../components/Button'

export default function ConferenceDetail() {
  const { id } = useParams()
  const [conference, setConference] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getConference(id)
      .then((res) => setConference(res.data))
      .catch(() => setError('컨퍼런스 정보를 불러오지 못했습니다.'))
  }, [id])

  if (error) return <p className="max-w-6xl mx-auto px-6 py-16 text-danger">{error}</p>
  if (!conference) return <p className="max-w-6xl mx-auto px-6 py-16 text-text-muted">불러오는 중...</p>

  const dateLabel = formatDateRange(conference.startAt, conference.endAt)

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <Link to="/conferences" className="text-sm text-text-muted hover:text-text">‹ 컨퍼런스 목록</Link>

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
        {conference.sessions.map((s) => (
          <div
            key={s.id}
            className="bg-surface border border-border rounded-xl p-5 flex items-center justify-between"
          >
            <div>
              <p className="text-text font-medium mb-1">{s.title}</p>
              <span className="text-sm text-text-muted">정원 {s.capacity}명</span>
            </div>
            <Link to={`/conferences/${conference.id}/sessions/${s.id}/apply`} state={{ session: s, conferenceTitle: conference.title }}>
              <Button>신청하기</Button>
            </Link>
          </div>
        ))}
        {conference.sessions.length === 0 && (
          <p className="text-text-muted text-sm py-8 text-center">아직 등록된 세션이 없어요.</p>
        )}
      </div>
    </div>
  )
}
