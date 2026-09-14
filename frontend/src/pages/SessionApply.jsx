import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { Minus, Plus, AlertTriangle } from 'lucide-react'
import Button from '../components/Button'
import SelectField from '../components/SelectField'
import { useAuth } from '../context/AuthContext'
import { createHold, getCapacityStatus } from '../api/reservations'
import { ApiError } from '../api/client'
import { AGE_GROUPS, JOBS } from '../utils/profileOptions'

// 세션마다 주최자가 정한 1인당 최대 신청 인원(maxHeadcountPerApplication)을 쓰고, 없는(구) 세션은 이 값으로 대체한다.
const DEFAULT_MAX_HEADCOUNT = 20

// 신청 인원이 이 값 이상이면 좌석별 개별 입력 대신 전체 일괄 입력으로 전환한다(요구사항 v0.5).
const GROUP_THRESHOLD = 10

function formatDateTime(value) {
  if (!value) return null
  const d = new Date(value)
  return `${d.getMonth() + 1}월 ${d.getDate()}일 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatDuration(startAt, endAt) {
  if (!startAt || !endAt) return null
  const minutes = Math.round((new Date(endAt) - new Date(startAt)) / 60000)
  return minutes > 0 ? `${minutes}분` : null
}

export default function SessionApply() {
  const { id: conferenceId, sessionId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { claims } = useAuth()
  const session = location.state?.session
  const conferenceTitle = location.state?.conferenceTitle
  const maxHeadcount = session?.maxHeadcountPerApplication ?? DEFAULT_MAX_HEADCOUNT

  const [headcount, setHeadcount] = useState(1)
  // 개별 입력(9명 이하): 좌석마다 연령대·직무. 일괄 입력(10명 이상): 전체 좌석에 같은 값 적용.
  const [attendees, setAttendees] = useState([{ ageGroup: '', job: '' }])
  const [batchAgeGroup, setBatchAgeGroup] = useState('')
  const [batchJob, setBatchJob] = useState('')
  const [capacity, setCapacity] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!sessionId) return
    getCapacityStatus(sessionId)
      .then((res) => setCapacity(res.data))
      .catch(() => {
        // 잔여 좌석 표시는 부가 정보라, 실패해도 신청 자체는 막지 않는다
      })
  }, [sessionId])

  const isGroup = headcount >= GROUP_THRESHOLD

  const changeHeadcount = (next) => {
    const clamped = Math.max(1, Math.min(maxHeadcount, next))
    setHeadcount(clamped)
    setAttendees((prev) => {
      const copy = prev.slice(0, clamped)
      while (copy.length < clamped) copy.push({ ageGroup: '', job: '' })
      return copy
    })
  }

  const updateAttendee = (index, field, value) => {
    setAttendees((prev) => prev.map((a, i) => (i === index ? { ...a, [field]: value } : a)))
  }

  const attendeesValid = isGroup
    ? Boolean(batchAgeGroup && batchJob)
    : attendees.length === headcount && attendees.every((a) => a.ageGroup && a.job)

  const submit = async () => {
    if (!attendeesValid) {
      setError('모든 참석자의 연령대·직무를 선택해주세요.')
      return
    }
    setError('')
    setLoading(true)
    try {
      const attendeesPayload = isGroup
        ? Array.from({ length: headcount }, () => ({ ageGroup: batchAgeGroup, job: batchJob }))
        : attendees
      const res = await createHold({ sessionId, memberId: claims.memberId, headcount, attendees: attendeesPayload })
      if (res.data.status === 'QUEUED') {
        navigate(`/reservations/${res.data.reservationId}/queue`, {
          state: { sessionTitle: session?.title, queuePosition: res.data.queuePosition },
        })
      } else {
        navigate(`/reservations/${res.data.reservationId}/payment`, {
          state: { sessionTitle: session?.title, conferenceTitle, headcount, price: session?.price ?? 0 },
        })
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '신청에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const remaining = capacity ? Math.max(capacity.remaining, 0) : null
  const percent = capacity?.capacity > 0 ? Math.min(100, Math.round((capacity.confirmedCount / capacity.capacity) * 100)) : null
  const nearFull = capacity?.capacity > 0 && remaining > 0 && remaining <= Math.max(1, Math.ceil(capacity.capacity * 0.1))

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <div className="max-w-xl mx-auto">
        <Link to={`/conferences/${conferenceId}`} className="text-sm text-text-muted hover:text-text">
          ‹ 세션 목록
        </Link>
      </div>

      <div className="max-w-xl mx-auto mt-4">
        <h1 className="text-2xl font-semibold text-text mb-1">세션 신청</h1>
        {conferenceTitle && <p className="text-text-muted mb-6">{conferenceTitle}</p>}

        <div className="bg-surface border border-border rounded-xl p-5 mb-4">
          <div className="flex items-start justify-between gap-3">
            <p className="text-text font-medium mb-1">{session?.title ?? '세션'}</p>
            {nearFull && (
              <span className="shrink-0 px-2.5 py-1 rounded-md text-xs font-medium text-warning bg-warning/10">
                마감 임박
              </span>
            )}
          </div>
          <p className="text-sm text-text-muted">
            {session?.speaker}
            {session?.speaker && session?.location && ' · '}
            {session?.location}
          </p>
          <p className="text-sm text-text-muted mt-1">
            {[formatDateTime(session?.sessionStartAt), formatDuration(session?.sessionStartAt, session?.sessionEndAt)]
              .filter(Boolean)
              .join(' · ')}
          </p>

          {capacity && (
            <div className="mt-3">
              <div className="flex items-center justify-between text-sm mb-1.5">
                <span className="text-warning">잔여 {remaining}석</span>
                <span className="text-text-muted">{percent}%</span>
              </div>
              <div className="h-1.5 rounded-full bg-surface2 overflow-hidden">
                <div className="h-full bg-warning rounded-full" style={{ width: `${percent}%` }} />
              </div>
            </div>
          )}
        </div>

        <div className="bg-surface border border-border rounded-xl p-5 mb-4">
          <p className="text-sm font-medium text-text mb-3">신청 인원</p>
          <div className="flex items-center gap-4">
            <button
              onClick={() => changeHeadcount(headcount - 1)}
              className="w-9 h-9 rounded-lg bg-surface2 border border-border text-text hover:bg-border flex items-center justify-center"
            >
              <Minus size={16} />
            </button>
            <span className="text-xl font-semibold text-text w-6 text-center">{headcount}</span>
            <button
              onClick={() => changeHeadcount(headcount + 1)}
              className="w-9 h-9 rounded-lg bg-surface2 border border-border text-text hover:bg-border flex items-center justify-center"
            >
              <Plus size={16} />
            </button>
            <span className="text-sm text-text-faint">최대 {maxHeadcount}인</span>
          </div>
        </div>

        <div className="bg-surface border border-border rounded-xl p-5 mb-4">
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm font-medium text-text">참석자 정보</p>
            <span className="text-xs text-text-muted">{isGroup ? `${GROUP_THRESHOLD}명 이상 — 일괄 입력` : '개별 입력 필수'}</span>
          </div>

          {isGroup ? (
            <>
              <p className="text-sm text-text-muted mb-3">전체 참석자에게 적용할 연령대·직무를 선택해 주세요.</p>
              <div className="grid grid-cols-2 gap-4">
                <SelectField label="연령대" placeholder="선택" options={AGE_GROUPS} value={batchAgeGroup} onChange={(e) => setBatchAgeGroup(e.target.value)} />
                <SelectField label="직무" placeholder="선택" options={JOBS} value={batchJob} onChange={(e) => setBatchJob(e.target.value)} />
              </div>
            </>
          ) : (
            <div className="space-y-3">
              {attendees.map((a, i) => (
                <div key={i} className="flex items-center gap-3">
                  <span className="w-16 shrink-0 text-sm text-text-muted">{i === 0 ? '본인' : `동반자 ${i}`}</span>
                  <div className="flex-1 grid grid-cols-2 gap-3">
                    <SelectField placeholder="연령대" options={AGE_GROUPS} value={a.ageGroup} onChange={(e) => updateAttendee(i, 'ageGroup', e.target.value)} />
                    <SelectField placeholder="직무" options={JOBS} value={a.job} onChange={(e) => updateAttendee(i, 'job', e.target.value)} />
                  </div>
                </div>
              ))}
            </div>
          )}

          <p className="flex items-center gap-1.5 text-xs text-text-muted mt-3">
            <AlertTriangle size={13} className="text-warning shrink-0" /> 모든 참석자의 연령대·직무를 선택해야 신청할 수 있습니다.
          </p>
        </div>

        {error && <p className="text-sm text-danger mb-3">{error}</p>}

        <div className="bg-surface border border-border rounded-xl p-3">
          <Button onClick={submit} loading={loading} disabled={!attendeesValid} className="w-full">
            신청하고 결제하기
          </Button>
        </div>
      </div>
    </div>
  )
}
