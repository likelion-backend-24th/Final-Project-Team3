import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'
import TextField from '../../components/TextField'
import Button from '../../components/Button'
import { createConference } from '../../api/conferences'
import { ApiError } from '../../api/client'

// 시안(주최자 - 컨퍼런스 등록)엔 있지만 Conference 엔티티엔 title/capacity뿐이라 저장은 안 되는 항목들 (배너 이미지는 제외 요청으로 뺌).
// 화면은 시안대로 맞추되, 실제 등록 API(POST /api/conferences)엔 title/capacity만 보낸다.
// 백엔드에 필드가 생기면 submit 쪽에 같이 실어 보내면 된다.
const CATEGORY_TAGS = ['Software', 'AI', 'ML', 'Cloud', 'Security', 'Frontend', 'Backend', 'DevOps', 'Mobile', 'Data', 'Career', 'Startup']

export default function ConferenceCreate() {
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [capacity, setCapacity] = useState('')
  const [dateRange, setDateRange] = useState('')
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
    setLoading(true)
    try {
      // 날짜/장소/소개/배너/태그는 백엔드에 저장할 곳이 없어서 여기서 보내지 않는다.
      await createConference({ title, capacity: Number(capacity) })
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

            <div className="grid grid-cols-2 gap-4">
              <TextField
                label="날짜 / 기간"
                placeholder="예: 2027-03-15 ~ 2027-03-17"
                value={dateRange}
                onChange={(e) => setDateRange(e.target.value)}
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

            <TextField
              label="개최 장소"
              placeholder="예: COEX 그랜드볼룸, 서울"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
            />

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
            {/* Link로 감싸면 취소 쪽 flex item 박스 모델이 미묘하게 달라져서 두 버튼 너비가 안 맞았음 — 버튼 자체를 flex item으로 맞춤 */}
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
