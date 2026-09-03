import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Search, Calendar, MapPin, ChevronRight } from 'lucide-react'
import { listConferences } from '../api/conferences'

const CATEGORIES = ['전체', '소프트웨어', 'AI', 'ML', '클라우드', '보안', '프런트엔드']

// 카드마다 다른 느낌을 주기 위한 결정론적 그라디언트 (실제 배너 이미지가 백엔드에 없어서 대체)
// to-surface로 끝나야 카드 하단 내용 영역(bg-surface)이랑 색이 정확히 이어져서 경계가 안 보인다 (to-bg로 끝내면 살짝 더 어두운
// 페이지 배경색으로 끝나버려서 그 아래 surface 배경이랑 미묘하게 어긋나 경계선처럼 보였음).
// 투명도를 너무 낮게 주면 색이 죽어서 카드가 다 비슷하게 어두워 보이길래, 히어로 글로우 정도 밝기로 다시 선명하게 올림.
const GRADIENTS = [
  'from-indigo-900/60 via-purple-900/35 to-surface',
  'from-blue-900/60 via-cyan-900/35 to-surface',
  'from-rose-900/60 via-fuchsia-900/35 to-surface',
  'from-emerald-900/60 via-teal-900/35 to-surface',
  'from-amber-900/60 via-orange-900/35 to-surface',
]

// ⚠️ 더미 표시용 데이터 — Conference 엔티티엔 title/capacity/status만 있고
// 날짜·장소·주최기관·세션 수·태그·마감임박 배지 필드는 백엔드에 없다 (docs: frontend-build-progress 메모 참고).
// 디자인 시안(Main 화면)과 비주얼을 맞추기 위한 임시 placeholder이며 실제 값이 아니다.
// 백엔드에 해당 필드/API가 추가되면 이 배열 대신 실제 응답값으로 교체할 것.
const DUMMY_DETAILS = [
  {
    organizer: '한국 개발자 협회',
    date: '2026년 9월 15일-17일',
    location: '코엑스 그랜드볼룸, 서울',
    sessions: 5,
    tags: ['소프트웨어', '오픈소스'],
    badges: ['대기열', '마감임박'],
  },
  {
    organizer: 'AI 연구원',
    date: '2026년 10월 3일-4일',
    location: '롯데호텔, 서울',
    sessions: 3,
    tags: ['AI', 'ML'],
    badges: ['대기열'],
  },
  {
    organizer: '한국정보보호학회',
    date: '2026년 12월 5일-6일',
    location: '동대문디자인플라자(DDP), 서울',
    sessions: 2,
    tags: ['보안', 'CTF'],
    badges: ['대기열'],
  },
  {
    organizer: '클라우드 네이티브 코리아',
    date: '2026년 11월 12일-13일',
    location: '벡스코, 부산',
    sessions: 4,
    tags: ['클라우드', 'DevOps'],
    badges: [],
  },
  {
    organizer: '프런트엔드 모임',
    date: '2026년 8월 22일',
    location: '위워크 강남, 서울',
    sessions: 6,
    tags: ['프런트엔드', 'UI'],
    badges: ['마감임박'],
  },
]

const BADGE_STYLES = {
  대기열: 'bg-danger text-white',
  마감임박: 'bg-warning text-white',
}

// GRADIENTS/DUMMY_DETAILS 인덱스 0~4가 같은 카테고리를 가리키게 맞춘 매핑.
// 제목에 카테고리 키워드가 있으면 그거랑 짝지어서 "소프트웨어 컨퍼런스인데 보안 태그·빨간 배너" 같은 어색한 조합을 피한다.
const CATEGORY_INDEX = { 소프트웨어: 0, AI: 1, ML: 1, 보안: 2, 클라우드: 3, 프런트엔드: 4 }

function hashId(id) {
  return [...id].reduce((sum, ch) => sum + ch.charCodeAt(0), 0)
}

function indexFor(conference) {
  const matchedCategory = Object.keys(CATEGORY_INDEX).find((cat) => conference.title.includes(cat))
  return matchedCategory !== undefined ? CATEGORY_INDEX[matchedCategory] : hashId(conference.id) % DUMMY_DETAILS.length
}

function gradientFor(conference) {
  return GRADIENTS[indexFor(conference)]
}

function dummyFor(conference) {
  return DUMMY_DETAILS[indexFor(conference)]
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
            const d = dummyFor(c)
            return (
              <Link
                key={c.id}
                to={`/conferences/${c.id}`}
                className="bg-surface border border-border rounded-xl overflow-hidden hover:border-primary transition-colors flex flex-col"
              >
                <div className={`relative h-32 bg-gradient-to-br ${gradientFor(c)}`} />
                <div className="p-5 flex flex-col flex-1">
                  <div className="flex flex-wrap gap-1.5 mb-3">
                    {d.tags.map((tag) => (
                      <span
                        key={tag}
                        className="px-2 py-1 rounded-md text-xs text-text-muted bg-surface2 border border-border"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>

                  <h3 className="text-text font-medium mb-1">{c.title}</h3>
                  <p className="text-text-muted text-sm mb-3">{d.organizer}</p>

                  <div className="space-y-1.5 text-sm text-text-muted">
                    <span className="flex items-center gap-1.5">
                      <Calendar size={14} /> {d.date}
                    </span>
                    <span className="flex items-center gap-1.5">
                      <MapPin size={14} /> {d.location}
                    </span>
                  </div>

                  <div className="mt-auto flex items-center justify-between pt-4 text-sm text-text-muted">
                    <span>{d.sessions}개 세션</span>
                    <div className="flex items-center gap-2">
                      {d.badges.map((badge) => (
                        <span
                          key={badge}
                          className={`px-2.5 py-1 rounded-md text-xs font-medium ${BADGE_STYLES[badge] ?? 'text-text-muted bg-surface2'}`}
                        >
                          {badge}
                        </span>
                      ))}
                      <ChevronRight size={16} />
                    </div>
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
