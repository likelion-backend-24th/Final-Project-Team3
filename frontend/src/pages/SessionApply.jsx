import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { Minus, Plus } from 'lucide-react'
import Button from '../components/Button'
import { useAuth } from '../context/AuthContext'
import { createHold } from '../api/reservations'
import { ApiError } from '../api/client'

const MAX_HEADCOUNT = 4

export default function SessionApply() {
  const { id: conferenceId, sessionId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { claims } = useAuth()
  const session = location.state?.session
  const conferenceTitle = location.state?.conferenceTitle

  const [headcount, setHeadcount] = useState(1)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    setError('')
    setLoading(true)
    try {
      const res = await createHold({ sessionId, memberId: claims.memberId, headcount })
      if (res.data.status === 'QUEUED') {
        navigate(`/reservations/${res.data.reservationId}/queue`, {
          state: { sessionTitle: session?.title, queuePosition: res.data.queuePosition },
        })
      } else {
        navigate(`/reservations/${res.data.reservationId}/complete`, {
          state: { sessionTitle: session?.title, conferenceTitle },
        })
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '신청에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <div className="max-w-xl mx-auto">
        <Link to={`/conferences/${conferenceId}`} className="text-sm text-text-muted hover:text-text">
          ‹ 세션 목록
        </Link>
      </div>

      <div className="max-w-xl mx-auto mt-4">
        <h1 className="text-2xl font-semibold text-text mb-1">세션 신청</h1>
        {conferenceTitle && <p className="text-text-muted mb-6">{conferenceTitle}</p>}

        <div className="bg-surface border border-border rounded-xl p-5 mb-4">
          <p className="text-text font-medium mb-1">{session?.title ?? '세션'}</p>
          <p className="text-sm text-text-muted">정원 {session?.capacity ?? '-'}명</p>
        </div>

        <div className="bg-surface border border-border rounded-xl p-5">
          <p className="text-sm font-medium text-text mb-3">신청 인원</p>
          <div className="flex items-center gap-4 mb-5">
            <button
              onClick={() => setHeadcount((n) => Math.max(1, n - 1))}
              className="w-9 h-9 rounded-lg bg-surface2 border border-border text-text hover:bg-border flex items-center justify-center"
            >
              <Minus size={16} />
            </button>
            <span className="text-xl font-semibold text-text w-6 text-center">{headcount}</span>
            <button
              onClick={() => setHeadcount((n) => Math.min(MAX_HEADCOUNT, n + 1))}
              className="w-9 h-9 rounded-lg bg-surface2 border border-border text-text hover:bg-border flex items-center justify-center"
            >
              <Plus size={16} />
            </button>
            <span className="text-sm text-text-faint">최대 {MAX_HEADCOUNT}인</span>
          </div>
          {error && <p className="text-sm text-danger mb-3">{error}</p>}
          <Button onClick={submit} loading={loading} className="w-full">
            신청하기
          </Button>
        </div>
      </div>
    </div>
  )
}
