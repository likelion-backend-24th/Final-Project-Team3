import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { listConferences } from '../../api/conferences'
import { useAuth } from '../../context/AuthContext'
import StatusBadge from '../../components/StatusBadge'
import Button from '../../components/Button'

export default function Dashboard() {
  const { claims } = useAuth()
  const [conferences, setConferences] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    listConferences()
      .then((res) => setConferences(res.data.filter((c) => c.organizerId === claims.memberId)))
      .catch(() => setError('컨퍼런스 목록을 불러오지 못했습니다.'))
  }, [claims.memberId])

  const pending = conferences?.filter((c) => c.status === 'PENDING').length ?? 0
  const approved = conferences?.filter((c) => c.status === 'APPROVED').length ?? 0

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-semibold text-text mb-1">주최자 대시보드</h1>
          <p className="text-text-muted">주최한 컨퍼런스와 운영 현황</p>
        </div>
        <Link to="/organizer/conferences/new">
          <Button className="inline-flex items-center gap-1.5">
            <Plus size={16} /> 컨퍼런스 등록
          </Button>
        </Link>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-10">
        <div className="bg-surface border border-border rounded-xl p-5">
          <p className="text-sm text-text-muted mb-1">전체 컨퍼런스</p>
          <p className="text-2xl font-semibold text-text">{conferences?.length ?? '-'}</p>
        </div>
        <div className="bg-surface border border-border rounded-xl p-5">
          <p className="text-sm text-text-muted mb-1">승인 대기</p>
          <p className="text-2xl font-semibold text-warning">{pending}</p>
        </div>
        <div className="bg-surface border border-border rounded-xl p-5">
          <p className="text-sm text-text-muted mb-1">승인됨</p>
          <p className="text-2xl font-semibold text-success">{approved}</p>
        </div>
      </div>

      <h2 className="text-sm font-medium text-text-muted mb-3">내 컨퍼런스</h2>
      {error && <p className="text-danger">{error}</p>}
      <div className="space-y-3">
        {conferences?.map((c) => (
          <div key={c.id} className="bg-surface border border-border rounded-xl p-5 flex items-center justify-between">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <p className="text-text font-medium">{c.title}</p>
                <StatusBadge status={c.status} />
              </div>
              <p className="text-sm text-text-muted">정원 {c.capacity}명</p>
            </div>
          </div>
        ))}
        {conferences?.length === 0 && (
          <p className="text-text-muted text-sm py-10 text-center">등록한 컨퍼런스가 아직 없어요.</p>
        )}
      </div>
    </div>
  )
}
