import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Building2, Calendar, MapPin, Sparkles } from 'lucide-react'
import { getOrganizerProfile } from '../api/organizers'
import { formatDateRange } from '../utils/date'

function ConferenceCard({ conference: c }) {
  return (
    <div className="bg-surface border border-border rounded-xl p-5">
      <Link to={`/conferences/${c.conferenceId}`} className="block">
        <p className="text-text font-medium hover:underline">{c.title}</p>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-text-muted mt-1">
          {formatDateRange(c.startAt, c.endAt) && (
            <span className="inline-flex items-center gap-1">
              <Calendar size={13} /> {formatDateRange(c.startAt, c.endAt)}
            </span>
          )}
          {c.location && (
            <span className="inline-flex items-center gap-1">
              <MapPin size={13} /> {c.location}
            </span>
          )}
        </div>
      </Link>
      {c.summaryText ? (
        <div className="mt-3 bg-surface2 border border-border rounded-lg p-3 flex gap-2">
          <Sparkles size={16} className="text-primary shrink-0 mt-0.5" />
          <p className="text-sm text-text-muted">{c.summaryText}</p>
        </div>
      ) : (
        <p className="text-sm text-text-faint mt-3">아직 AI 요약이 준비되지 않았어요.</p>
      )}
    </div>
  )
}

export default function OrganizerProfile() {
  const { organizerId } = useParams()
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getOrganizerProfile(organizerId)
      .then((res) => setProfile(res.data))
      .catch(() => setError('주최자 정보를 불러오지 못했습니다.'))
  }, [organizerId])

  if (error) return <p className="max-w-4xl mx-auto px-6 py-16 text-danger">{error}</p>
  if (!profile) return <p className="max-w-4xl mx-auto px-6 py-16 text-text-muted">불러오는 중...</p>

  const { pastConferences, ongoingConferences } = profile

  return (
    <div className="max-w-4xl mx-auto px-6 py-10">
      <button onClick={() => navigate(-1)} className="text-sm text-text-muted hover:text-text">‹ 뒤로</button>

      <div className="mt-4 mb-8 bg-surface border border-border rounded-xl p-6 flex gap-4 items-start">
        <div className="w-12 h-12 rounded-lg bg-primary/15 text-primary flex items-center justify-center shrink-0">
          <Building2 size={22} />
        </div>
        <div>
          <h1 className="text-xl font-semibold text-text">{profile.organizerName ?? '주최자'}</h1>
          <p className="text-sm text-text-muted mt-0.5">지난 컨퍼런스 {pastConferences.length}회</p>
        </div>
      </div>

      {ongoingConferences.length > 0 && (
        <div className="mb-8">
          <h2 className="text-sm font-medium text-text-muted mb-3">진행중·예정 컨퍼런스</h2>
          <div className="space-y-3">
            {ongoingConferences.map((c) => (
              <ConferenceCard key={c.conferenceId} conference={c} />
            ))}
          </div>
        </div>
      )}

      <h2 className="text-sm font-medium text-text-muted mb-3">지난 컨퍼런스</h2>
      <div className="space-y-3">
        {pastConferences.map((c) => (
          <ConferenceCard key={c.conferenceId} conference={c} />
        ))}
        {pastConferences.length === 0 && (
          <p className="text-text-muted text-sm py-8 text-center">아직 지난 컨퍼런스가 없어요.</p>
        )}
      </div>
    </div>
  )
}
