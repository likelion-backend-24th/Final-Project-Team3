import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Search, Calendar, MapPin, ChevronRight } from 'lucide-react'
import { listConferences } from '../api/conferences'
import { formatDateRange } from '../utils/date'

const CATEGORIES = ['전체', '소프트웨어', 'AI', 'ML', '클라우드', '보안', '프런트엔드']

// 카드마다 다른 느낌을 주기 위한 결정론적 그라디언트 (실제 배너 이미지가 백엔드에 없어서 대체)
// to-surface로 끝나야 카드 하단 내용 영역(bg-surface)이랑 색이 정확히 이어져서 경계가 안 보인다.
const GRADIENTS = [
  'from-indigo-900/60 via-purple-900/35 to-surface',
  'from-blue-900/60 via-cyan-900/35 to-surface',
  'from-rose-900/60 via-fuchsia-900/35 to-surface',
  'from-emerald-900/60 via-teal-900/35 to-surface',
  'from-amber-900/60 via-orange-900/35 to-surface',
]

function hashId(id) {
  return [...id].reduce((sum, ch) => sum + ch.charCodeAt(0), 0)
}

function gradientFor(id) {
  return GRADIENTS[hashId(id) % GRADIENTS.length]
}

// 태그 배지는 더 이상 더미가 아니라, 제목에 실제로 카테고리 키워드가 들어있는지 봐서 만든다
// (카테고리 필터가 하는 것과 같은 방식 — 지어낸 값이 아니라 실제 title에서 뽑아낸 값).
function tagsFor(title) {
  return CATEGORIES.filter((cat) => cat !== '전체' && title.includes(cat))
}

export default function Home() {
  const [conferences, setConferences] = useState([])
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('전체')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    listConferences()
      .then((res) => setConferences(res.data))
      .catch(() => setError('컨퍼런스 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [])

  const filtered = useMemo(
    () =>
      conferences.filter(
        (c) =>
          c.title.toLowerCase().includes(query.toLowerCase()) &&
          (category === '전체' || c.title.includes(category)),
      ),
    [conferences, query, category],
  )

  return (
    <div>
      <section className="max-w-6xl mx-auto px-6 pt-10">
        <div className="relative overflow-hidden rounded-xl bg-bg px-10 py-14">
          {/* 장식용 글로우 (이미지 아님) */}
          <div className="absolute -right-24 top-1/3 w-[40rem] h-[40rem] rounded-full bg-primary/20 blur-3xl" />
          <div className="absolute right-0 bottom-0 w-[28rem] h-[28rem] rounded-full bg-accent/10 blur-3xl" />

          <div className="relative">
            <span className="inline-flex items-center gap-2 text-xs text-accent mb-4 tracking-wide">
              <span className="w-1.5 h-1.5 rounded-full bg-accent" /> TECH CONFERENCE PLATFORM
            </span>
            <h1 className="text-4xl font-semibold text-text leading-tight mb-4">
              관심 있는 컨퍼런스,
              <br />
              <span className="text-primary">지금 바로</span> 신청하세요
            </h1>
            <p className="text-text-muted">
              최신 IT 컨퍼런스와 세션을 검색하고,
              <br />
              QR 티켓으로 간편하게 입장하세요.
            </p>
          </div>
        </div>
      </section>

      <div className="max-w-6xl mx-auto px-6 py-10">
        <div className="flex flex-col sm:flex-row gap-3 mb-8">
          <div className="relative flex-1">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-text-faint" size={18} />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="컨퍼런스 이름, 주제 검색..."
              className="w-full bg-surface border border-border rounded-lg pl-11 pr-4 py-3 text-sm text-text placeholder:text-text-faint focus:outline-none focus:border-primary"
            />
          </div>
          <div className="flex flex-wrap gap-2">
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                onClick={() => setCategory(cat)}
                className={`px-4 py-2 rounded-lg text-sm whitespace-nowrap border transition-colors ${
                  category === cat
                    ? 'bg-primary border-primary text-white'
                    : 'bg-surface border-border text-text-muted hover:text-text'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {loading && <p className="text-text-muted">불러오는 중...</p>}
        {error && <p className="text-danger">{error}</p>}

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map((c) => {
            const tags = tagsFor(c.title)
            const dateLabel = formatDateRange(c.startAt, c.endAt)
            return (
              <Link
                key={c.id}
                to={`/conferences/${c.id}`}
                className="bg-surface border border-border rounded-xl overflow-hidden hover:border-primary transition-colors flex flex-col"
              >
                <div className={`relative h-32 bg-gradient-to-br ${gradientFor(c.id)}`} />
                <div className="p-5 flex flex-col flex-1">
                  {tags.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 mb-3">
                      {tags.map((tag) => (
                        <span
                          key={tag}
                          className="px-2 py-1 rounded-md text-xs text-text-muted bg-surface2 border border-border"
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  )}

                  <h3 className="text-text font-medium mb-1">{c.title}</h3>
                  {c.organizerName && <p className="text-text-muted text-sm mb-3">{c.organizerName}</p>}

                  <div className="space-y-1.5 text-sm text-text-muted">
                    {dateLabel && (
                      <span className="flex items-center gap-1.5">
                        <Calendar size={14} /> {dateLabel}
                      </span>
                    )}
                    {c.location && (
                      <span className="flex items-center gap-1.5">
                        <MapPin size={14} /> {c.location}
                      </span>
                    )}
                  </div>

                  <div className="mt-auto flex items-center justify-end pt-4">
                    <ChevronRight size={16} className="text-text-muted" />
                  </div>
                </div>
              </Link>
            )
          })}
        </div>

        {!loading && filtered.length === 0 && (
          <p className="text-text-muted text-center py-16">조건에 맞는 컨퍼런스가 없어요.</p>
        )}
      </div>
    </div>
  )
}
