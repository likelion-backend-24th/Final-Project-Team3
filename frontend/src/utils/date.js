// conference-service가 주는 LocalDateTime 문자열(예: "2027-03-15T09:00:00")을
// "2026년 9월 15일-17일" 같은 한국어 날짜 범위 표기로 바꾼다.
export function formatDateRange(startAt, endAt) {
  if (!startAt) return null
  const start = new Date(startAt)
  const end = endAt ? new Date(endAt) : null
  const y = start.getFullYear()
  const m = start.getMonth() + 1
  const d = start.getDate()

  if (!end || end.toDateString() === start.toDateString()) {
    return `${y}년 ${m}월 ${d}일`
  }
  if (end.getFullYear() === y && end.getMonth() + 1 === m) {
    return `${y}년 ${m}월 ${d}일-${end.getDate()}일`
  }
  if (end.getFullYear() === y) {
    return `${y}년 ${m}월 ${d}일 - ${end.getMonth() + 1}월 ${end.getDate()}일`
  }
  return `${y}년 ${m}월 ${d}일 - ${end.getFullYear()}년 ${end.getMonth() + 1}월 ${end.getDate()}일`
}
