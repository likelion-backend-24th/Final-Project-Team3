import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'
import TextField from '../../components/TextField'
import Button from '../../components/Button'
import { createConference } from '../../api/conferences'
import { ApiError } from '../../api/client'

// PR #57/#65로 conference-service에 organizerName/startAt/endAt/location/description/tags가
// 전부 실제로 저장되게 됐다 (예전엔 title/capacity만 보냈음). 배너 이미지는 시안에 있었지만 제외 요청으로 안 만듦.
const CATEGORY_TAGS = ['Software', 'AI', 'ML', 'Cloud', 'Security', 'Frontend', 'Backend', 'DevOps', 'Mobile', 'Data', 'Career', 'Startup']

// datetime-local 인풋 값("2027-03-15T09:00")엔 초가 없어서 백엔드 LocalDateTime 파싱용으로 붙여준다.
function toLocalDateTime(value) {
  return value ? `${value}:00` : null
}

export default function ConferenceCreate() {
  const navigate = useNavigate()
  const [organizerName, setOrganizerName] = useState('')
  const [title, setTitle] = useState('')
  const [capacity, setCapacity] = useState('')
  const [startAt, setStartAt] = useState('')
  const [endAt, setEndAt] = useState('')
  const [location, setLocation] = useState('')
  const [description, setDescription] = useState('')
  const [tags, setTags] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const toggleTag = (tag) => {
    setTags((prev) => (prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]))
  }

  const submit = async (e) => {
    e.preventDefault()
    setError('')

    if (startAt && endAt && new Date(endAt) <= new Date(startAt)) {
      setError('종료 일시는 시작 일시보다 늦어야 합니다.')
      return
    }

    setLoading(true)
    try {
      await createConference({
        organizerName,
        title,
        capacity: Number(capacity),
        startAt: toLocalDateTime(startAt),
        endAt: toLocalDateTime(endAt),
        location,
        description: description || null,
        tags,
      })
      navigate('/organizer', { state: { justCreated: true } })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '등록 신청에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <div className="max-w-2xl mx-auto">
        <Link to="/organizer" className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text">
          <ChevronLeft size={16} /> 대시보드
        </Link>
      </div>

      <div className="max-w-2xl mx-auto mt-4">
        <h1 className="text-2xl font-semibold text-text mb-1">컨퍼런스 등록 신청</h1>
        <p className="text-text-muted mb-6">전체관리자 승인 후 세션을 설정할 수 있습니다</p>

        <form onSubmit={submit}>
          <div className="bg-surface border border-border rounded-xl p-6 space-y-5">
            <TextField
              label="컨퍼런스명"
              placeholder="예: DevCon 2027"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />

            <TextField
              label="주최기관명"
              placeholder="예: 한국 개발자 협회"
              value={organizerName}
              onChange={(e) => setOrganizerName(e.target.value)}
              required
            />

            <div className="grid grid-cols-2 gap-4">
              <TextField
                label="시작 일시"
                type="datetime-local"
                value={startAt}
                onChange={(e) => setStartAt(e.target.value)}
                required
              />
              <TextField
                label="종료 일시"
                type="datetime-local"
                value={endAt}
                onChange={(e) => setEndAt(e.target.value)}
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <TextField
                label="개최 장소"
                placeholder="예: COEX 그랜드볼룸, 서울"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                required
              />
              <TextField
                label="정원"
                type="number"
                min={1}
                placeholder="예: 200"
                value={capacity}
                onChange={(e) => setCapacity(e.target.value)}
                required
              />
            </div>

            <label className="block">
              <span className="block mb-2 text-sm text-text">컨퍼런스 소개</span>
              <textarea
                rows={4}
                placeholder="컨퍼런스 주제, 대상, 예상 참가자 수 등을 적어주세요."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full bg-bg border border-border rounded-lg px-4 py-3 text-sm text-text placeholder:text-text-faint focus:outline-none focus:border-primary resize-none"
              />
            </label>
          </div>

          <div className="bg-surface border border-border rounded-xl p-6 mt-6">
            <h2 className="text-sm font-medium text-text mb-3">카테고리 태그</h2>
            <div className="flex flex-wrap gap-2">
              {CATEGORY_TAGS.map((tag) => (
                <button
                  key={tag}
                  type="button"
                  onClick={() => toggleTag(tag)}
                  className={`px-3 py-1.5 rounded-lg text-sm border transition-colors ${
                    tags.includes(tag)
                      ? 'bg-primary border-primary text-white'
                      : 'bg-surface2 border-border text-text-muted hover:text-text'
                  }`}
                >
                  {tag}
                </button>
              ))}
            </div>
          </div>

          {error && <p className="text-sm text-danger mt-4">{error}</p>}

          <div className="flex gap-3 mt-6">
            <Button type="button" variant="secondary" className="flex-1" onClick={() => navigate('/organizer')}>
              취소
            </Button>
            <Button type="submit" loading={loading} className="flex-1">
              승인 신청
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
