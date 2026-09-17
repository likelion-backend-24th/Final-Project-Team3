import { useEffect, useState } from 'react'
import { getSettlementDashboard } from '../../api/admin'
import { ApiError } from '../../api/client'
import TextField from '../../components/TextField'
import Button from '../../components/Button'

function won(n) {
  return `₩${(n ?? 0).toLocaleString()}`
}

export default function SettlementDashboard() {
  const [range, setRange] = useState({ startDate: '', endDate: '' })
  const [totalAmount, setTotalAmount] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = (params) => {
    setLoading(true)
    setError('')
    getSettlementDashboard(params)
      .then((res) => setTotalAmount(res.data.totalAmount))
      .catch((err) => setError(err instanceof ApiError ? err.message : '정산 데이터를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => load({}), [])

  const submit = (e) => {
    e.preventDefault()
    load(range)
  }

  return (
    <div className="max-w-4xl mx-auto px-6 py-10">
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
        <Button type="submit" loading={loading}>
          조회
        </Button>
      </form>

      {error && <p className="text-sm text-danger mb-4">{error}</p>}

      <div className="bg-surface border border-border rounded-xl p-6 max-w-sm">
        <p className="text-sm text-text-muted mb-1">
          {range.startDate || range.endDate
            ? `${range.startDate || '전체'} ~ ${range.endDate || '전체'} 결제 금액`
            : '전체 기간 결제 금액'}
        </p>
        <p className="text-3xl font-semibold text-text">{loading ? '...' : won(totalAmount)}</p>
      </div>

      <p className="text-xs text-text-faint mt-4">
        GMV·수수료·주최자별 정산·월별 추이는 현재 API(`GET /api/admin/settlements`)가 결제 확정 총액 하나만
        내려줘서 표시하지 않아요.
      </p>
    </div>
  )
}
