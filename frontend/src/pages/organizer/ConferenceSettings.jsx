import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronLeft, X } from 'lucide-react'
import {
  getConference,
  updateDescription,
  updateLocation,
  listNotices,
  createNotice,
  deleteNotice,
  listFaqs,
  createFaq,
  deleteFaq,
} from '../../api/conferences'
import { ApiError } from '../../api/client'
import { formatDateRange } from '../../utils/date'
import Button from '../../components/Button'
import TextField from '../../components/TextField'

const TABS = [
  { key: 'description', label: '소개글' },
  { key: 'location', label: '장소' },
  { key: 'notices', label: '공지 / 안내' },
  { key: 'faqs', label: '문의사항' },
]

// 백엔드 공통 textarea 스타일 - TextField는 input 전용이라 여기선 직접 클래스를 맞춘다.
const textareaClass =
  'w-full bg-bg border border-border rounded-lg px-4 py-3 text-sm text-text placeholder:text-text-faint focus:outline-none focus:border-primary resize-none'

export default function ConferenceSettings() {
  const { id: conferenceId } = useParams()
  const [tab, setTab] = useState('description')
  const [conference, setConference] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getConference(conferenceId)
      .then((res) => setConference(res.data))
      .catch(() => setError('컨퍼런스 정보를 불러오지 못했습니다.'))
  }, [conferenceId])

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <div className="max-w-2xl mx-auto">
        <Link to="/organizer" className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text">
          <ChevronLeft size={16} /> 대시보드
        </Link>
      </div>

      <div className="max-w-2xl mx-auto mt-4 mb-6">
        <h1 className="text-2xl font-semibold text-text mb-1">컨퍼런스 설정</h1>
        <p className="text-text-muted">
          {conference ? `${conference.title} · ${formatDateRange(conference.startAt, conference.endAt)}` : ""}
        </p>
      </div>

      <div className="max-w-2xl mx-auto">
        <div className="flex gap-1 border-b border-border mb-6">
          {TABS.map((t) => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
                tab === t.key ? 'border-primary text-text' : 'border-transparent text-text-muted hover:text-text'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {error && <p className="text-sm text-danger mb-4">{error}</p>}
        {!conference && !error && <p className="text-text-muted text-sm">불러오는 중...</p>}

        {conference && tab === 'description' && <DescriptionTab conferenceId={conferenceId} conference={conference} />}
        {conference && tab === 'location' && <LocationTab conferenceId={conferenceId} conference={conference} />}
        {conference && tab === 'notices' && <NoticesTab conferenceId={conferenceId} />}
        {conference && tab === 'faqs' && <FaqsTab conferenceId={conferenceId} />}
      </div>
    </div>
  )
}

function SaveFeedback({ error, message }) {
  return (
    <>
      {error && <p className="text-sm text-danger mt-3">{error}</p>}
      {message && <p className="text-sm text-success mt-3">{message}</p>}
    </>
  )
}

function DescriptionTab({ conferenceId, conference }) {
  const [description, setDescription] = useState(conference.description ?? '')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const save = async () => {
    setSaving(true)
    setError('')
    setMessage('')
    try {
      await updateDescription(conferenceId, description)
      setMessage('저장됐어요.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="bg-surface border border-border rounded-xl p-6">
      <h2 className="text-sm font-medium text-text mb-3">컨퍼런스 소개글</h2>
      <textarea
        rows={8}
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        placeholder="참가자에게 보여줄 컨퍼런스 소개를 적어주세요."
        className={textareaClass}
      />
      <SaveFeedback error={error} message={message} />
      <Button className="mt-4" onClick={save} loading={saving}>
        저장
      </Button>
    </div>
  )
}

function LocationTab({ conferenceId, conference }) {
  const [form, setForm] = useState({
    location: conference.location ?? '',
    transportation: conference.transportation ?? '',
    parkingInfo: conference.parkingInfo ?? '',
    amenities: conference.amenities ?? '',
  })
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  // 승인된 컨퍼런스는 주소만 잠긴다(백엔드 CONFERENCE_LOCATION_ADDRESS_LOCKED) - 교통편/주차/편의시설은 계속 수정 가능.
  const addressLocked = conference.status === 'APPROVED'

  const setField = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const save = async () => {
    setSaving(true)
    setError('')
    setMessage('')
    try {
      await updateLocation(conferenceId, form)
      setMessage('저장됐어요.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="bg-surface border border-border rounded-xl p-6 space-y-5">
      <h2 className="text-sm font-medium text-text -mb-2">장소 정보</h2>

      <div>
        <TextField
          label="주소"
          value={form.location}
          onChange={setField('location')}
          disabled={addressLocked}
        />
        {addressLocked && (
          <p className="text-xs text-text-faint mt-1.5">승인된 컨퍼런스는 주소를 변경할 수 없어요.</p>
        )}
      </div>

      <TextField
        label="교통편"
        placeholder="예: 지하철 2호선 삼성역 5번 출구 도보 5분"
        value={form.transportation}
        onChange={setField('transportation')}
      />
      <TextField
        label="주차 안내"
        placeholder="예: 현장 유료 주차 가능 (1일 최대 ₩15,000)"
        value={form.parkingInfo}
        onChange={setField('parkingInfo')}
      />
      <TextField
        label="편의시설"
        placeholder="예: 수유실, 장애인 화장실, 엘리베이터 완비"
        value={form.amenities}
        onChange={setField('amenities')}
      />

      <div>
        <span className="block mb-2 text-sm text-text">지도 미리보기</span>
        <div className="h-40 rounded-lg bg-surface2 border border-border flex items-center justify-center text-sm text-text-faint">
          준비 중이에요
        </div>
      </div>

      <SaveFeedback error={error} message={message} />
      <Button onClick={save} loading={saving}>
        저장
      </Button>
    </div>
  )
}

function NoticesTab({ conferenceId }) {
  const [notices, setNotices] = useState(null)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const load = () => {
    listNotices(conferenceId)
      .then((res) => setNotices(res.data))
      .catch(() => setError('공지 목록을 불러오지 못했습니다.'))
  }

  useEffect(load, [conferenceId])

  const submit = async (e) => {
    e.preventDefault()
    if (!title.trim() || !content.trim()) return
    setSubmitting(true)
    setError('')
    try {
      await createNotice(conferenceId, { title, content })
      setTitle('')
      setContent('')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '공지 등록에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const remove = async (noticeId) => {
    setError('')
    try {
      await deleteNotice(conferenceId, noticeId)
      setNotices((list) => list.filter((n) => n.id !== noticeId))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '삭제에 실패했습니다.')
    }
  }

  return (
    <div className="space-y-4">
      <form onSubmit={submit} className="bg-surface border border-border rounded-xl p-6 space-y-3">
        <h2 className="text-sm font-medium text-text mb-1">새 공지 작성</h2>
        <TextField placeholder="공지 제목" value={title} onChange={(e) => setTitle(e.target.value)} />
        <textarea
          rows={4}
          placeholder="공지 내용"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          className={textareaClass}
        />
        {error && <p className="text-sm text-danger">{error}</p>}
        <Button type="submit" loading={submitting}>
          공지 등록
        </Button>
      </form>

      {notices?.map((n) => (
        <div key={n.id} className="bg-surface border border-border rounded-xl p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-text font-medium">{n.title}</p>
              <p className="text-xs text-text-faint mt-0.5">{formatDateRange(n.createdAt)}</p>
            </div>
            <button
              onClick={() => remove(n.id)}
              className="text-text-faint hover:text-danger shrink-0"
              aria-label="공지 삭제"
            >
              <X size={16} />
            </button>
          </div>
          <p className="text-sm text-text-muted mt-2 whitespace-pre-wrap">{n.content}</p>
        </div>
      ))}
      {notices?.length === 0 && <p className="text-text-muted text-sm text-center py-8">등록된 공지가 없어요.</p>}
    </div>
  )
}

function FaqsTab({ conferenceId }) {
  const [faqs, setFaqs] = useState(null)
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const load = () => {
    listFaqs(conferenceId)
      .then((res) => setFaqs(res.data))
      .catch(() => setError('FAQ 목록을 불러오지 못했습니다.'))
  }

  useEffect(load, [conferenceId])

  const submit = async (e) => {
    e.preventDefault()
    if (!question.trim() || !answer.trim()) return
    setSubmitting(true)
    setError('')
    try {
      await createFaq(conferenceId, { question, answer })
      setQuestion('')
      setAnswer('')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'FAQ 등록에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const remove = async (faqId) => {
    setError('')
    try {
      await deleteFaq(conferenceId, faqId)
      setFaqs((list) => list.filter((f) => f.id !== faqId))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '삭제에 실패했습니다.')
    }
  }

  return (
    <div className="space-y-4">
      <form onSubmit={submit} className="bg-surface border border-border rounded-xl p-6 space-y-3">
        <h2 className="text-sm font-medium text-text mb-1">새 FAQ 추가</h2>
        <TextField placeholder="질문" value={question} onChange={(e) => setQuestion(e.target.value)} />
        <textarea
          rows={3}
          placeholder="답변"
          value={answer}
          onChange={(e) => setAnswer(e.target.value)}
          className={textareaClass}
        />
        {error && <p className="text-sm text-danger">{error}</p>}
        <Button type="submit" loading={submitting}>
          FAQ 추가
        </Button>
      </form>

      {faqs?.map((f) => (
        <div key={f.id} className="bg-surface border border-border rounded-xl p-5">
          <div className="flex items-start justify-between gap-3">
            <p className="text-text font-medium">{f.question}</p>
            <button
              onClick={() => remove(f.id)}
              className="text-text-faint hover:text-danger shrink-0"
              aria-label="FAQ 삭제"
            >
              <X size={16} />
            </button>
          </div>
          <p className="text-sm text-text-muted mt-2 whitespace-pre-wrap">{f.answer}</p>
        </div>
      ))}
      {faqs?.length === 0 && <p className="text-text-muted text-sm text-center py-8">등록된 FAQ가 없어요.</p>}
    </div>
  )
}
