import { useEffect, useState } from 'react'
import { getMyConferences, getSessionsByConference } from '../../api/conferences'
import { formatDateRange } from '../../utils/date'
import StatusBadge from '../../components/StatusBadge'

// 컨퍼런스 등록과 별개로, "내가 신청한 게 지금 어떤 상태인지"만 한곳에 모아 보여주는 화면.
// 세션도 컨퍼런스처럼 승인 절차를 거치니 두 신청 내역을 같이 둔다.
export default function Applications() {
  const [conferences, setConferences] = useState(null)
  const [sessions, setSessions] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getMyConferences()
      .then((res) => {
        setConferences(res.data)
        // 세션은 "내 세션 전체"를 한 번에 주는 API가 없어서, 내 컨퍼런스마다 세션 목록을 훑어 합친다.
        return Promise.allSettled(res.data.map((c) => getSessionsByConference(c.id)))
      })
      .then((results) => {
        setSessions(results.flatMap((r) => (r.status === 'fulfilled' ? r.value.data : [])))
      })
      .catch(() => setError('신청 내역을 불러오지 못했습니다.'))
  }, [])

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-semibold text-text mb-1">신청 현황</h1>
      <p className="text-text-muted mb-8">컨퍼런스·세션 등록 신청 내역을 한곳에서 확인하세요</p>

      {error && <p className="text-sm text-danger mb-4">{error}</p>}

      <section className="mb-10">
        <h2 className="text-sm font-medium text-text-muted mb-3">컨퍼런스 신청 내역</h2>
        <div className="space-y-3">
          {conferences?.map((c) => (
            <div key={c.id} className="bg-surface border border-border rounded-xl p-5">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-text font-medium">{c.title}</p>
                  <p className="text-sm text-text-muted mt-0.5">{formatDateRange(c.startAt, c.endAt)}</p>
                </div>
                <StatusBadge status={c.status} />
              </div>
              {c.status === 'REJECTED' && c.rejectionReason && (
                <p className="text-sm text-danger mt-2">반려 사유: {c.rejectionReason}</p>
              )}
            </div>
          ))}
          {conferences?.length === 0 && (
            <p className="text-text-muted text-sm py-8 text-center">신청한 컨퍼런스가 없어요.</p>
          )}
          {!conferences && !error && <p className="text-text-muted text-sm">불러오는 중...</p>}
        </div>
      </section>

      <section>
        <h2 className="text-sm font-medium text-text-muted mb-3">세션 신청 내역</h2>
        <div className="space-y-3">
          {sessions?.map((s) => (
            <div key={s.id} className="bg-surface border border-border rounded-xl p-5">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-text font-medium">{s.title}</p>
                  <p className="text-sm text-text-muted mt-0.5">
                    {s.conferenceTitle} · {formatDateRange(s.sessionStartAt, s.sessionEndAt)}
                  </p>
                </div>
                <StatusBadge status={s.status} />
              </div>
              {s.status === 'REJECTED' && s.rejectionReason && (
                <p className="text-sm text-danger mt-2">반려 사유: {s.rejectionReason}</p>
              )}
            </div>
          ))}
          {sessions?.length === 0 && (
            <p className="text-text-muted text-sm py-8 text-center">신청한 세션이 없어요.</p>
          )}
          {!sessions && !error && <p className="text-text-muted text-sm">불러오는 중...</p>}
        </div>
      </section>
    </div>
  )
}
