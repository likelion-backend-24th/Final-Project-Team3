import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronLeft, RefreshCw, Sparkles } from 'lucide-react'
import { getAttendeeSummary } from '../../api/conferences'
import { ApiError } from '../../api/client'

const AGE_LABELS = {
  TEENS: '10대',
  TWENTIES: '20대',
  THIRTIES: '30대',
  FORTIES: '40대',
  FIFTIES_OR_OLDER: '50대 이상',
}

// 카테고리별 고정 색상(다크 배경 기준 CVD 검증 완료 - dataviz 스킬 categorical palette dark열).
// 값 크기로 재배정하지 않고 항목에 고정으로 묶어서, 어떤 차트를 봐도 "개발자=파랑"이 항상 같게 유지한다.
const AGE_COLORS = {
  TEENS: '#3987e5',
  TWENTIES: '#d95926',
  THIRTIES: '#199e70',
  FORTIES: '#c98500',
  FIFTIES_OR_OLDER: '#d55181',
}

const JOB_LABELS = {
  DEVELOPER: '개발자',
  DESIGNER: '디자이너',
  PLANNER_PM: '기획/PM',
  MARKETING_SALES: '마케팅/영업',
  DATA_AI: '데이터/AI',
  STUDENT: '학생',
  OTHER: '기타',
}

const JOB_COLORS = {
  DEVELOPER: '#3987e5',
  DESIGNER: '#d95926',
  PLANNER_PM: '#199e70',
  MARKETING_SALES: '#c98500',
  DATA_AI: '#d55181',
  STUDENT: '#008300',
  OTHER: '#9085e9',
}

// stroke-dasharray로 그리는 도넛. 세그먼트 사이는 배경색(카드 surface)이 그대로 비치는 간격(gap)으로 분리해서
// 따로 테두리를 안 그려도 구분돼 보이게 한다.
function Donut({ data, total, size = 140, thickness = 20 }) {
  const r = (size - thickness) / 2
  const c = size / 2
  const circumference = 2 * Math.PI * r
  const gap = 3
  let cumulative = 0

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} role="img" aria-label="분포 도넛 차트">
      <g transform={`rotate(-90 ${c} ${c})`}>
        {total === 0 ? (
          <circle cx={c} cy={c} r={r} fill="none" stroke="currentColor" className="text-surface2" strokeWidth={thickness} />
        ) : (
          data.map((d) => {
            const fraction = d.count / total
            const segment = fraction * circumference
            const dash = Math.max(segment - gap, 0)
            const dashArray = `${dash} ${circumference - dash}`
            const dashOffset = -cumulative
            cumulative += segment
            return (
              <circle
                key={d.key}
                cx={c}
                cy={c}
                r={r}
                fill="none"
                stroke={d.color}
                strokeWidth={thickness}
                strokeDasharray={dashArray}
                strokeDashoffset={dashOffset}
              />
            )
          })
        )}
      </g>
      <text x={c} y={c} textAnchor="middle" dominantBaseline="central" fill="currentColor" className="text-text text-xl font-semibold">
        {total}
      </text>
    </svg>
  )
}

function DistributionDonut({ title, distribution, labels, colors, total }) {
  const data = Object.entries(distribution)
    .map(([key, count]) => ({ key, count, label: labels[key] ?? key, color: colors[key] ?? '#898781' }))
    .sort((a, b) => b.count - a.count)

  return (
    <div>
      <h2 className="text-sm font-medium text-text mb-4">{title}</h2>
      <div className="flex items-center gap-6">
        <Donut data={data} total={total} />
        <ul className="flex-1 space-y-2 min-w-0">
          {data.map((d) => {
            const percent = total > 0 ? Math.round((d.count / total) * 100) : 0
            return (
              <li key={d.key} className="flex items-center justify-between gap-3 text-sm">
                <span className="flex items-center gap-2 text-text-muted truncate">
                  <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: d.color }} />
                  {d.label}
                </span>
                <span className="text-text-faint shrink-0">
                  {d.count}명 · {percent}%
                </span>
              </li>
            )
          })}
        </ul>
      </div>
    </div>
  )
}

export default function AttendeeSummary() {
  const { id: conferenceId } = useParams()
  const [summary, setSummary] = useState(null)
  const [error, setError] = useState('')
  const [refreshing, setRefreshing] = useState(false)

  const load = useCallback(
    (isRefresh) => {
      if (isRefresh) setRefreshing(true)
      setError('')
      getAttendeeSummary(conferenceId)
        .then((res) => setSummary(res.data))
        .catch((err) => setError(err instanceof ApiError ? err.message : '참석자 통계를 불러오지 못했습니다.'))
        .finally(() => setRefreshing(false))
    },
    [conferenceId],
  )

  useEffect(() => load(false), [load])

  return (
    <div className="max-w-3xl mx-auto px-6 py-10">
      <Link to="/organizer" className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text">
        <ChevronLeft size={16} /> 대시보드
      </Link>

      <div className="flex items-start justify-between gap-4 mt-4 mb-1">
        <h1 className="text-2xl font-semibold text-text">참석자 통계</h1>
        <button
          onClick={() => load(true)}
          disabled={refreshing}
          className="inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text disabled:opacity-50 shrink-0 mt-1"
        >
          <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} /> 새로고침
        </button>
      </div>
      {summary && (
        <p className="text-text-muted mb-1">
          체크인 완료된 참석자 {summary.checkedInCount}명 기준 통계와 AI 요약이에요.
        </p>
      )}
      <p className="text-xs text-text-faint mb-8">
        체크인 수가 바뀌지 않았으면 이전에 생성해둔 결과를 그대로 보여줘요 (AI 재호출 없음).
      </p>

      {error && <p className="text-danger">{error}</p>}
      {!summary && !error && <p className="text-text-muted">불러오는 중...</p>}

      {summary && (
        <div className="space-y-6">
          {summary.checkedInCount > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <DistributionDonut
                title="연령대 분포"
                distribution={summary.ageGroupDistribution}
                labels={AGE_LABELS}
                colors={AGE_COLORS}
                total={summary.checkedInCount}
              />
              <DistributionDonut
                title="직무 분포"
                distribution={summary.jobDistribution}
                labels={JOB_LABELS}
                colors={JOB_COLORS}
                total={summary.checkedInCount}
              />
            </div>
          )}

          <div className="bg-surface border border-border rounded-xl p-6">
            <h2 className="text-sm font-medium text-text mb-3 flex items-center gap-1.5">
              <Sparkles size={16} className="text-primary" /> AI 요약
            </h2>
            <p className="text-sm text-text-muted whitespace-pre-wrap">{summary.summaryText}</p>
            {summary.generatedAt && (
              <p className="text-xs text-text-faint mt-3">
                생성 시각: {new Date(summary.generatedAt).toLocaleString('ko-KR')}
              </p>
            )}
          </div>

          <Link
            to={`/organizer/conferences/${conferenceId}/reviews`}
            className="block text-center text-sm text-text-muted hover:text-text bg-surface border border-border rounded-xl p-4"
          >
            후기 원문 전체 보기 →
          </Link>
        </div>
      )}
    </div>
  )
}
