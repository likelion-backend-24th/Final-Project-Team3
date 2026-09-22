import { useEffect, useState } from 'react'
import { getSettlementDashboard, getSettlementDetails } from '../../api/admin'
import { listConferences, getConference } from '../../api/conferences'
import { ApiError } from '../../api/client'
import TextField from '../../components/TextField'
import Button from '../../components/Button'
import StatusBadge from '../../components/StatusBadge'

function won(n) {
  return `₩${(n ?? 0).toLocaleString()}`
}

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  return `${d.getMonth() + 1}월 ${d.getDate()}일 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const PAGE_SIZE = 20

export default function SettlementDashboard() {
  const [range, setRange] = useState({ startDate: '', endDate: '' })
  const [totalAmount, setTotalAmount] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [sessionMap, setSessionMap] = useState({})
  const [details, setDetails] = useState([])
  const [page, setPage] = useState(0)
  const [pageMeta, setPageMeta] = useState(null)
  const [detailsLoading, setDetailsLoading] = useState(true)
  const [detailsError, setDetailsError] = useState('')

  // sessionId -> {title, conferenceTitle}. 응답에 세션·컨퍼런스 이름이 없어서
  // 마이페이지와 같은 방식으로 공개 컨퍼런스 목록을 훑어 매핑한다.
  useEffect(() => {
    listConferences()
      .then((confRes) => Promise.all(confRes.data.map((c) => getConference(c.id))))
      .then((details) => {
        const map = {}
        details.forEach((d) =>
          d.data.sessions.forEach((s) => {
            map[s.id] = { title: s.title, conferenceTitle: d.data.title }
          }),
        )
        setSessionMap(map)
      })
      .catch(() => {
        // 매핑 실패해도 세부 목록은 세션 ID로라도 표시한다
      })
  }, [])

  const load = (params) => {
    setLoading(true)
    setError('')
    getSettlementDashboard(params)
      .then((res) => setTotalAmount(res.data.totalAmount))
      .catch((err) => setError(err instanceof ApiError ? err.message : '정산 데이터를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }

  const loadDetails = (params, targetPage) => {
    setDetailsLoading(true)
    setDetailsError('')
    getSettlementDetails({ ...params, page: targetPage, size: PAGE_SIZE })
      .then((res) => {
        setDetails(res.data)
        setPageMeta(res.meta?.pagination ?? null)
      })
      .catch((err) => setDetailsError(err instanceof ApiError ? err.message : '정산 내역을 불러오지 못했습니다.'))
      .finally(() => setDetailsLoading(false))
  }

  useEffect(() => {
    load({})
    loadDetails({}, 0)
  }, [])

  const submit = (e) => {
    e.preventDefault()
    setPage(0)
    load(range)
    loadDetails(range, 0)
  }

  const goToPage = (next) => {
    setPage(next)
    loadDetails(range, next)
  }

  return (
    <div className="max-w-5xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-semibold text-text mb-1">플랫폼 정산 대시보드</h1>
      <p className="text-text-muted mb-8">전체 결제 현황</p>

      <form onSubmit={submit} className="flex flex-wrap items-end gap-3 mb-6">
        <TextField
          label="시작일"
          type="date"
          value={range.startDate}
          onChange={(e) => setRange((r) => ({ ...r, startDate: e.target.value }))}
        />
        <TextField
          label="종료일"
          type="date"
          value={range.endDate}
          onChange={(e) => setRange((r) => ({ ...r, endDate: e.target.value }))}
        />
        <Button type="submit" loading={loading || detailsLoading}>
          조회
        </Button>
      </form>

      {error && <p className="text-sm text-danger mb-4">{error}</p>}

      <div className="bg-surface border border-border rounded-xl p-6 max-w-sm mb-10">
        <p className="text-sm text-text-muted mb-1">
          {range.startDate || range.endDate
            ? `${range.startDate || '전체'} ~ ${range.endDate || '전체'} 결제 금액`
            : '전체 기간 결제 금액'}
        </p>
        <p className="text-3xl font-semibold text-text">{loading ? '...' : won(totalAmount)}</p>
      </div>

      <h2 className="text-lg font-semibold text-text mb-3">결제 내역 상세</h2>

      {detailsError && <p className="text-sm text-danger mb-4">{detailsError}</p>}
      {!detailsError && details.length === 0 && !detailsLoading && (
        <p className="text-text-faint text-sm py-8 text-center">해당 기간의 결제 내역이 없어요.</p>
      )}

      {details.length > 0 && (
        <div className="bg-surface border border-border rounded-xl overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-text-faint text-xs">
                <th className="text-left font-medium px-4 py-3">세션</th>
                <th className="text-left font-medium px-4 py-3">결제일시</th>
                <th className="text-right font-medium px-4 py-3">금액</th>
                <th className="text-left font-medium px-4 py-3">상태</th>
                <th className="text-right font-medium px-4 py-3">환불액</th>
                <th className="text-right font-medium px-4 py-3">체크인</th>
              </tr>
            </thead>
            <tbody>
              {details.map((d) => {
                const session = sessionMap[d.sessionId]
                return (
                  <tr key={d.reservationId} className="border-b border-border last:border-0">
                    <td className="px-4 py-3">
                      <p className="text-text">{session?.title ?? `세션 #${d.sessionId.slice(0, 8)}`}</p>
                      <p className="text-text-faint text-xs mt-0.5">{session?.conferenceTitle ?? '-'}</p>
                    </td>
                    <td className="px-4 py-3 text-text-muted">{formatDateTime(d.paidAt)}</td>
                    <td className="px-4 py-3 text-right text-text">{won(d.amount)}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={d.reservationStatus} />
                    </td>
                    <td className="px-4 py-3 text-right text-text-muted">
                      {d.refundedAmount != null ? won(d.refundedAmount) : '-'}
                    </td>
                    <td className="px-4 py-3 text-right text-text-muted">
                      {d.checkedInCount}/{d.ticketCount}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {pageMeta && pageMeta.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 mt-4">
          <Button variant="secondary" disabled={!pageMeta.hasPrev} onClick={() => goToPage(page - 1)}>
            이전
          </Button>
          <span className="text-sm text-text-muted">
            {page + 1} / {pageMeta.totalPages}
          </span>
          <Button variant="secondary" disabled={!pageMeta.hasNext} onClick={() => goToPage(page + 1)}>
            다음
          </Button>
        </div>
      )}
    </div>
  )
}
