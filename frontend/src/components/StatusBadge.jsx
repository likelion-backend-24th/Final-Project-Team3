const styles = {
  PENDING: 'text-warning bg-warning/10',
  APPROVED: 'text-success bg-success/10',
  REJECTED: 'text-danger bg-danger/10',
  HOLD: 'text-success bg-success/10',
  QUEUED: 'text-danger bg-danger/10',
}

const labels = {
  PENDING: '승인 대기',
  APPROVED: '승인됨',
  REJECTED: '반려됨',
  HOLD: '신청 완료',
  QUEUED: '대기열 등록',
}

export default function StatusBadge({ status }) {
  return (
    <span className={`px-2.5 py-1 rounded-md text-xs font-medium ${styles[status] ?? 'text-text-muted bg-surface2'}`}>
      {labels[status] ?? status}
    </span>
  )
}
