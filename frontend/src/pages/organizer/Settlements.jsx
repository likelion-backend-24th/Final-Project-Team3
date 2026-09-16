import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'
import { getMyConferences, getSettlement } from '../../api/conferences'
import { ApiError } from '../../api/client'

function won(n) {
  return `₩${(n ?? 0).toLocaleString()}`
}

export default function Settlements() {
  const [rows, setRows] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getMyConferences()
      .then((res) => {
        const approved = res.data.filter((c) => c.status === 'APPROVED')
        return Promise.allSettled(approved.map((c) => getSettlement(c.id).then((r) => r.data)))
      })
      .then((results) => {
        setRows(results.filter((r) => r.status === 'fulfilled').map((r) => r.value))
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : '정산 내역을 불러오지 못했습니다.'))
  }, [])

  const totals = (rows ?? []).reduce(
    (acc, r) => ({
      totalRevenue: acc.totalRevenue + r.totalRevenue,
      refundedAmount: acc.refundedAmount + r.refundedAmount,
      netRevenue: acc.netRevenue + r.netRevenue,
    }),
    { totalRevenue: 0, refundedAmount: 0, netRevenue: 0 },
  )

  return (
    <div className="max-w-4xl mx-auto px-6 py-10">
      <Link to="/organizer" className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text mb-4">
        <ChevronLeft size={16} /> 대시보드
      </Link>
      <h1 className="text-2xl font-semibold text-text mb-1">정산 내역</h1>
      <p className="text-text-muted mb-8">컨퍼런스별 매출 및 정산 현황</p>

      {error && <p className="text-sm text-danger mb-4">{error}</p>}
      {!rows && !error && <p className="text-text-muted text-sm">불러오는 중...</p>}

      {rows && (
        <>
          <div className="grid grid-cols-3 gap-4 mb-8">
            <div className="bg-surface border border-border rounded-xl p-5">
              <p className="text-sm text-text-muted mb-1">총 매출</p>
              <p className="text-xl font-semibold text-text">{won(totals.totalRevenue)}</p>
            </div>
            <div className="bg-surface border border-border rounded-xl p-5">
              <p className="text-sm text-text-muted mb-1">환불액</p>
              <p className="text-xl font-semibold text-danger">{won(totals.refundedAmount)}</p>
            </div>
            <div className="bg-surface border border-border rounded-xl p-5">
              <p className="text-sm text-text-muted mb-1">순매출</p>
              <p className="text-xl font-semibold text-success">{won(totals.netRevenue)}</p>
            </div>
          </div>

          <div className="bg-surface border border-border rounded-xl divide-y divide-border">
            <div className="p-5">
              <h2 className="text-sm font-medium text-text">컨퍼런스별 정산</h2>
            </div>
            {rows.length === 0 && <p className="text-text-muted text-sm text-center py-10">승인된 컨퍼런스가 없어요.</p>}
            {rows.map((r) => (
              <div key={r.conferenceId} className="p-5 flex items-center justify-between gap-4">
                <div>
                  <p className="text-text font-medium">{r.conferenceTitle}</p>
                  <p className="text-xs text-text-faint mt-1">
                    확정 {r.confirmedCount}건 · 취소 {r.cancelledCount}건
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-text font-medium">{won(r.netRevenue)}</p>
                  <p className="text-xs text-text-faint">매출 {won(r.totalRevenue)} · 환불 {won(r.refundedAmount)}</p>
                </div>
              </div>
            ))}
          </div>

          <p className="text-xs text-text-faint mt-4">
            월별 매출 추이·정산 완료 여부는 아직 API에 없어서 표시하지 않아요.
          </p>
        </>
      )}
    </div>
  )
}
