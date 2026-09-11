import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft, Info } from 'lucide-react'
import TextField from '../../components/TextField'
import Button from '../../components/Button'
import { createSession, getConference, getSessionsByConference, updateSession } from '../../api/conferences'
import { ApiError } from '../../api/client'
import { formatDateRange } from '../../utils/date'

// 세션 생성/수정 공용 폼. 수정 모드(sessionId가 있으면)는 title을 못 바꾸고(백엔드 SessionUpdateRequest에
// title이 없음), 저장하면 백엔드가 status를 무조건 PENDING으로 리셋한다(재승인 정책) — 그 사실을 안내한다.
function toLocalDateTime(value) {
  return value ? `${value}:00` : null
}

// LocalDateTime 문자열("2027-03-15T09:00:00")을 datetime-local 인풋 값으로.
function toInputValue(value) {
  return value ? value.slice(0, 16) : ''
}

const emptyForm = {
  capacity: '',
  startAt: '',
  endAt: '',
  sessionStartAt: '',
  sessionEndAt: '',
  location: '',
  speaker: '',
  price: '0',
  maxHeadcountPerApplication: '',
}

function formFromSession(session) {
  return {
    capacity: String(session.capacity ?? ''),
    startAt: toInputValue(session.startAt),
    endAt: toInputValue(session.endAt),
    sessionStartAt: toInputValue(session.sessionStartAt),
    sessionEndAt: toInputValue(session.sessionEndAt),
    location: session.location ?? '',
    speaker: session.speaker ?? '',
    price: String(session.price ?? '0'),
    maxHeadcountPerApplication: String(session.maxHeadcountPerApplication ?? ''),
  }
}

export default function SessionCreate() {
  const { id: conferenceId, sessionId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const isEdit = Boolean(sessionId)

  const [title, setTitle] = useState(location.state?.session?.title ?? '')
  const [form, setForm] = useState(location.state?.session ? formFromSession(location.state.session) : emptyForm)
  const [conference, setConference] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [loadingSession, setLoadingSession] = useState(isEdit && !location.state?.session)

  // 세션 진행 일시가 컨퍼런스 진행 기간 밖으로 나가지 않게 미리 막으려면 컨퍼런스 기간을 알아야 한다.
  // (세션 등록은 승인된 컨퍼런스에서만 가능하니 공개 상세 조회로 충분하다.)
  useEffect(() => {
    getConference(conferenceId)
      .then((res) => setConference(res.data))
      .catch(() => {})
  }, [conferenceId])

  // 직접 URL로 들어와서 location.state가 없는 경우(새로고침 등)를 위한 폴백 —
  // 세션 단건 조회 API가 없어서 목록에서 찾는다.
  useEffect(() => {
    if (!isEdit || location.state?.session) return
    getSessionsByConference(conferenceId)
      .then((res) => {
        const session = res.data.find((s) => s.id === sessionId)
        if (session) {
          setTitle(session.title)
          setForm(formFromSession(session))
        } else {
          setError('세션 정보를 찾을 수 없어요.')
        }
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : '세션 정보를 불러오지 못했습니다.'))
      .finally(() => setLoadingSession(false))
  }, [isEdit, conferenceId, sessionId, location.state])

  const setField = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setError('')

    if (form.startAt && form.endAt && new Date(form.endAt) <= new Date(form.startAt)) {
      setError('신청 종료 일시는 시작 일시보다 늦어야 합니다.')
      return
    }
    if (form.sessionStartAt && form.sessionEndAt && new Date(form.sessionEndAt) <= new Date(form.sessionStartAt)) {
      setError('진행 종료 일시는 시작 일시보다 늦어야 합니다.')
      return
    }
    if (Number(form.maxHeadcountPerApplication) > Number(form.capacity)) {
      setError('1인당 최대 신청 인원은 정원을 초과할 수 없습니다.')
      return
    }
    if (
      conference &&
      form.sessionStartAt &&
      form.sessionEndAt &&
      (new Date(form.sessionStartAt) < new Date(conference.startAt) || new Date(form.sessionEndAt) > new Date(conference.endAt))
    ) {
      setError('세션 진행 일시는 컨퍼런스 진행 기간 안에 있어야 합니다.')
      return
    }

    const payload = {
      capacity: Number(form.capacity),
      startAt: toLocalDateTime(form.startAt),
      endAt: toLocalDateTime(form.endAt),
      sessionStartAt: toLocalDateTime(form.sessionStartAt),
      sessionEndAt: toLocalDateTime(form.sessionEndAt),
      location: form.location,
      speaker: form.speaker,
      price: Number(form.price) || 0,
      maxHeadcountPerApplication: Number(form.maxHeadcountPerApplication),
    }

    setLoading(true)
    try {
      if (isEdit) {
        await updateSession(sessionId, payload)
        navigate(`/organizer/conferences/${conferenceId}/sessions`, { state: { justUpdatedSession: true } })
      } else {
        await createSession(conferenceId, { title, ...payload })
        navigate(`/organizer/conferences/${conferenceId}/sessions`, { state: { justCreatedSession: true } })
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : `세션 ${isEdit ? '수정' : '등록'}에 실패했습니다.`)
    } finally {
      setLoading(false)
    }
  }

  if (loadingSession) {
    return <div className="max-w-6xl mx-auto px-6 py-20 text-center text-text-muted">불러오는 중...</div>
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
        <h1 className="text-2xl font-semibold text-text mb-1">세션 {isEdit ? '수정' : '등록'}</h1>
        <p className="text-text-muted mb-6">
          {isEdit ? '수정 내용은 전체관리자 재승인 후 반영됩니다' : '전체관리자 승인 후 참가자에게 공개됩니다'}
        </p>

        {isEdit && (
          <div className="flex gap-2 bg-warning/10 text-warning text-sm rounded-lg px-3 py-2.5 mb-5">
            <Info size={16} className="shrink-0 mt-0.5" />
            <span>수정하고 저장하면 승인 상태였더라도 다시 승인 대기로 바뀌어요. 승인 전까지 참가자 화면에서 숨겨집니다.</span>
          </div>
        )}

        <form onSubmit={submit}>
          <div className="bg-surface border border-border rounded-xl p-6 space-y-5">
            {isEdit ? (
              <div>
                <span className="block mb-2 text-sm text-text">세션명</span>
                <p className="text-text-muted text-sm px-4 py-3 bg-bg border border-border rounded-lg">{title}</p>
              </div>
            ) : (
              <TextField
                label="세션명"
                placeholder="예: 키노트: 스택을 넘어서"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
              />
            )}

            <div className="grid grid-cols-2 gap-4">
              <TextField
                label="정원"
                type="number"
                min={1}
                placeholder="예: 60"
                value={form.capacity}
                onChange={setField('capacity')}
                required
              />
              <TextField
                label="참가 비용(원)"
                type="number"
                min={0}
                placeholder="0"
                value={form.price}
                onChange={setField('price')}
                required
              />
            </div>

            <TextField
              label="1인당 최대 신청 인원"
              type="number"
              min={1}
              placeholder="예: 4"
              value={form.maxHeadcountPerApplication}
              onChange={setField('maxHeadcountPerApplication')}
              required
            />

            <div className="grid grid-cols-2 gap-4">
              <TextField
                label="신청 시작 일시"
                type="datetime-local"
                value={form.startAt}
                onChange={setField('startAt')}
                required
              />
              <TextField
                label="신청 종료 일시"
                type="datetime-local"
                value={form.endAt}
                onChange={setField('endAt')}
                required
              />
            </div>

            <div>
              <div className="grid grid-cols-2 gap-4">
                <TextField
                  label="진행 시작 일시"
                  type="datetime-local"
                  value={form.sessionStartAt}
                  onChange={setField('sessionStartAt')}
                  required
                />
                <TextField
                  label="진행 종료 일시"
                  type="datetime-local"
                  value={form.sessionEndAt}
                  onChange={setField('sessionEndAt')}
                  required
                />
              </div>
              {conference && (
                <p className="text-xs text-text-faint mt-2">
                  컨퍼런스 진행 기간({formatDateRange(conference.startAt, conference.endAt)}) 안에서 설정해주세요.
                </p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <TextField
                label="장소"
                placeholder="예: 그랜드홀 A"
                value={form.location}
                onChange={setField('location')}
                required
              />
              <TextField
                label="발표자"
                placeholder="예: 김연수 CTO"
                value={form.speaker}
                onChange={setField('speaker')}
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
              {isEdit ? '수정 저장' : '세션 등록'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
