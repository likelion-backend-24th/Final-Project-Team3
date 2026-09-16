import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronLeft, MessageSquare } from 'lucide-react'
import { getReviews } from '../../api/conferences'
import { ApiError } from '../../api/client'

export default function ReviewList() {
  const { id: conferenceId } = useParams()
  const [reviews, setReviews] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getReviews(conferenceId)
      .then((res) => setReviews(res.data))
      .catch((err) => setError(err instanceof ApiError ? err.message : '후기 목록을 불러오지 못했습니다.'))
  }, [conferenceId])

  return (
    <div className="max-w-3xl mx-auto px-6 py-10">
      <Link to="/organizer" className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text">
        <ChevronLeft size={16} /> 대시보드
      </Link>

      <h1 className="text-2xl font-semibold text-text mt-4 mb-1">후기 목록</h1>
      <p className="text-text-muted mb-8">
        체크인 완료된 참석자가 남긴 후기 원문이에요. 작성자 정보는 표시되지 않아요.
      </p>

      {error && <p className="text-danger">{error}</p>}
      {!reviews && !error && <p className="text-text-muted">불러오는 중...</p>}

      {reviews && (
        <div className="space-y-3">
          {reviews.map((content, i) => (
            <div key={i} className="bg-surface border border-border rounded-xl p-5 flex gap-3">
              <MessageSquare size={16} className="text-text-faint shrink-0 mt-0.5" />
              <p className="text-sm text-text whitespace-pre-wrap">{content}</p>
            </div>
          ))}
          {reviews.length === 0 && (
            <p className="text-text-muted text-sm py-16 text-center">아직 등록된 후기가 없어요.</p>
          )}
        </div>
      )}
    </div>
  )
}
