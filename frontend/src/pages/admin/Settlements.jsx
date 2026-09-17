import { useEffect, useState } from 'react'
import { getSettlementDashboard } from '../../api/admin'
import { ApiError } from '../../api/client'
import Button from '../../components/Button'

const inputClass =
  'bg-bg border border-border rounded-lg px-4 py-2.5 text-sm text-text placeholder:text-text-faint focus:outline-none focus:border-primary'

export default function Settlements() {
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [totalAmount, setTotalAmount] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = async (filters) => {
    setLoading(true)
    setError('')
    try {
      const res = await getSettlementDashboard(filters)
      setTotalAmount(res.data.totalAmount)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '정산 정보를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load({})
  }, [])

  const submit = (e) => {
    e.preventDefault()
    load({ startDate: startDate || undefined, endDate: endDate || undefined })
  }

  const reset = () => {
    setStartDate('')
    setEndDate('')
    load({})
  }

  return (
    <div className="max-w-2xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-semibold text-text">통합 정산 대시보드</h1>
      <p className="text-sm text-text-muted mt-1">
        결제 완료(CONFIRMED)된 예약의 매출만 집계합니다. 취소된 예약은 자동으로 제외돼요.
      </p>

      <form onSubmit={submit} className="mt-6 flex flex-wrap items-end gap-3">
        <div>
          <label className="text-xs text-text-muted block mb-1">시작일</label>
          <input type="date" className={inputClass} value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-text-muted block mb-1">종료일</label>
          <input type="date" className={inputClass} value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </div>
        <Button type="submit" variant="secondary" loading={loading}>
          조회
        </Button>
        {(startDate || endDate) && (
          <button type="button" onClick={reset} className="text-sm text-text-muted hover:text-text underline">
            기간 초기화
          </button>
        )}
      </form>

      <div className="mt-8 bg-surface border border-border rounded-xl p-8">
        <p className="text-sm text-text-muted">
          {startDate || endDate ? `${startDate || '처음'} ~ ${endDate || '지금'} 매출 합계` : '전체 기간 매출 합계'}
        </p>
        {error ? (
          <p className="text-sm text-danger mt-3">{error}</p>
        ) : (
          <p className="text-4xl font-semibold text-text mt-3">
            {loading ? '조회 중...' : `${(totalAmount ?? 0).toLocaleString()}원`}
          </p>
        )}
      </div>
    </div>
  )
}
