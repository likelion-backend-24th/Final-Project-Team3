import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Users, Calendar, Clock, MapPin, Mic } from 'lucide-react'
import { getConference, listNotices, listFaqs } from '../api/conferences'
import { getCapacityStatus } from '../api/reservations'
import { formatDateRange } from '../utils/date'
import StatusBadge from '../components/StatusBadge'
import Button from '../components/Button'
import DescriptionText from '../components/DescriptionText'

const INFO_TABS = [
  { key: 'sessions', label: '세션 목록' },
  { key: 'description', label: '소개글' },
  { key: 'location', label: '장소' },
  { key: 'notices', label: '공지 / 안내' },
  { key: 'faqs', label: '문의사항' },
]

export default function ConferenceDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [conference, setConference] = useState(null)
  const [capacityBySession, setCapacityBySession] = useState({})
  const [notices, setNotices] = useState(null)
  const [faqs, setFaqs] = useState(null)
  const [error, setError] = useState('')
  const [infoTab, setInfoTab] = useState('sessions')

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

    // 공지/FAQ 조회(GET)는 공개 API라 로그인 여부와 무관하게 방문자도 볼 수 있다.
    listNotices(id).then((res) => setNotices(res.data)).catch(() => setNotices([]))
    listFaqs(id).then((res) => setFaqs(res.data)).catch(() => setFaqs([]))
  }, [id])

  if (error) return <p className="max-w-6xl mx-auto px-6 py-16 text-danger">{error}</p>
  if (!conference) return <p className="max-w-6xl mx-auto px-6 py-16 text-text-muted">불러오는 중...</p>

  const dateLabel = formatDateRange(conference.startAt, conference.endAt)

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <button onClick={() => navigate(-1)} className="text-sm text-text-muted hover:text-text">‹ 뒤로</button>

      {conference.detailImageUrl && (
        <div
          className="mt-4 h-48 rounded-xl bg-cover bg-center"
          style={{ backgroundImage: `url(${conference.detailImageUrl})` }}
        />
      )}

      <div className="mt-4 mb-8">
        <div className="flex items-center gap-3 mb-1">
          <h1 className="text-2xl font-semibold text-text">{conference.title}</h1>
          <StatusBadge status={conference.status} />
        </div>
        {conference.organizerName && (
          <Link to={`/organizers/${conference.organizerId}`} className="inline-block mb-3 group">
            <p className="text-text-muted group-hover:text-text transition-colors">{conference.organizerName}</p>
            {conference.organizerPastConferenceCount > 0 && (
              <p className="text-xs text-text-faint mt-0.5">
                지난 컨퍼런스 {conference.organizerPastConferenceCount}회
                {conference.organizerRepresentativeSummary && ` · ${conference.organizerRepresentativeSummary}`}
              </p>
            )}
          </Link>
        )}

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
      </div>

      <div className="mb-4">
        <div className="flex gap-1 border-b border-border mb-6">
          {INFO_TABS.map((t) => (
            <button
              key={t.key}
              onClick={() => setInfoTab(t.key)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
                infoTab === t.key ? 'border-primary text-text' : 'border-transparent text-text-muted hover:text-text'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {infoTab === 'sessions' && (
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
                      {s.startAt && (
                        <span className="inline-flex items-center gap-1">
                          <Clock size={13} /> 신청 {formatDateRange(s.startAt, s.endAt)}
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
        )}

        {infoTab === 'description' && (
          <div className="bg-surface border border-border rounded-xl p-6">
            {conference.aiSummary && (
              <p className="text-sm text-text bg-surface2 border border-border rounded-lg px-4 py-3 mb-4">
                {conference.aiSummary}
              </p>
            )}
            {conference.description ? (
              <DescriptionText text={conference.description} className="text-sm text-text-muted whitespace-pre-wrap" />
            ) : (
              <p className="text-text-muted text-sm">등록된 소개글이 없어요.</p>
            )}
          </div>
        )}

        {infoTab === 'location' && (
          <div className="bg-surface border border-border rounded-xl p-6 space-y-1.5 text-sm text-text-muted">
            {conference.location && <p>주소: {conference.location}</p>}
            {conference.transportation && <p>교통편: {conference.transportation}</p>}
            {conference.parkingInfo && <p>주차 안내: {conference.parkingInfo}</p>}
            {conference.amenities && <p>편의시설: {conference.amenities}</p>}
            {!conference.location && !conference.transportation && !conference.parkingInfo && !conference.amenities && (
              <p>등록된 장소 정보가 없어요.</p>
            )}
          </div>
        )}

        {infoTab === 'notices' && (
          <div className="space-y-3">
            {notices === null && <p className="text-text-muted text-sm">불러오는 중...</p>}
            {notices?.length === 0 && <p className="text-text-muted text-sm">등록된 공지가 없어요.</p>}
            {notices?.map((n) => (
              <div key={n.id} className="bg-surface border border-border rounded-xl p-5">
                <p className="text-text font-medium">{n.title}</p>
                <p className="text-xs text-text-faint mt-0.5">{formatDateRange(n.createdAt)}</p>
                <p className="text-sm text-text-muted mt-2 whitespace-pre-wrap">{n.content}</p>
              </div>
            ))}
          </div>
        )}

        {infoTab === 'faqs' && (
          <div className="space-y-3">
            {faqs === null && <p className="text-text-muted text-sm">불러오는 중...</p>}
            {faqs?.length === 0 && <p className="text-text-muted text-sm">등록된 FAQ가 없어요.</p>}
            {faqs?.map((f) => (
              <div key={f.id} className="bg-surface border border-border rounded-xl p-5">
                <p className="text-text font-medium">{f.question}</p>
                <p className="text-sm text-text-muted mt-2 whitespace-pre-wrap">{f.answer}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
