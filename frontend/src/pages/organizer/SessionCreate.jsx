import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'
import TextField from '../../components/TextField'
import Button from '../../components/Button'
import { createSession } from '../../api/conferences'
import { ApiError } from '../../api/client'

// 세션 "목록 조회" API가 아직 없어서(주최자 소유 스코프로 전체 상태를 볼 방법이 없음) 등록 폼만 제공한다.
// 등록 후엔 대시보드로 돌아가며, 방금 등록한 세션이 승인 대기 상태라는 안내만 보여준다.
function toLocalDateTime(value) {
  return value ? `${value}:00` : null
}

export default function SessionCreate() {
  const { id: conferenceId } = useParams()
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [capacity, setCapacity] = useState('')
  const [startAt, setStartAt] = useState('')
  const [endAt, setEndAt] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')

    if (startAt && endAt && new Date(endAt) <= new Date(startAt)) {
      setError('종료 일시는 시작 일시보다 늦어야 합니다.')
      return
    }

    setLoading(true)
    try {
      await createSession(conferenceId, {
        title,
        capacity: Number(capacity),
        startAt: toLocalDateTime(startAt),
        endAt: toLocalDateTime(endAt),
      })
      navigate(`/organizer/conferences/${conferenceId}/sessions`, { state: { justCreatedSession: true } })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '세션 등록에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <div className="max-w-xl mx-auto">
        <Link
          to={`/organizer/conferences/${conferenceId}/sessions`}
          className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text"
        >
          <ChevronLeft size={16} /> 세션 관리
        </Link>
      </div>

      <div className="max-w-xl mx-auto mt-4">
        <h1 className="text-2xl font-semibold text-text mb-1">세션 등록</h1>
        <p className="text-text-muted mb-6">전체관리자 승인 후 참가자에게 공개됩니다</p>

        <form onSubmit={submit}>
          <div className="bg-surface border border-border rounded-xl p-6 space-y-5">
            <TextField
              label="세션명"
              placeholder="예: 키노트: 스택을 넘어서"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />

            <TextField
              label="정원"
              type="number"
              min={1}
              placeholder="예: 60"
              value={capacity}
              onChange={(e) => setCapacity(e.target.value)}
              required
            />

            <div className="grid grid-cols-2 gap-4">
              <TextField
                label="신청 시작 일시"
                type="datetime-local"
                value={startAt}
                onChange={(e) => setStartAt(e.target.value)}
                required
              />
              <TextField
                label="신청 종료 일시"
                type="datetime-local"
                value={endAt}
                onChange={(e) => setEndAt(e.target.value)}
                required
              />
            </div>
          </div>

          {error && <p className="text-sm text-danger mt-4">{error}</p>}

          <div className="flex gap-3 mt-6">
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              onClick={() => navigate(`/organizer/conferences/${conferenceId}/sessions`)}
            >
              취소
            </Button>
            <Button type="submit" loading={loading} className="flex-1">
              세션 등록
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
