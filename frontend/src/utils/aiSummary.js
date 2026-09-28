// 참석자 AI 요약이 실패했거나 체크인이 없으면 서버가 요약 대신 안내 문구를 저장해둔다
// (conference-service ConferenceAttendeeSummaryService의 LLM_FAILURE_MESSAGE / ZERO_CHECKIN_MESSAGE).
// 화면에서는 이걸 요약처럼 보여주지 않고 "아직 요약 없음"으로 취급한다.
const PLACEHOLDERS = ['요약 정보를 일시적으로 생성하지 못했습니다.', '아직 체크인한 참석자가 없습니다.']

export function isRealSummary(text) {
  return Boolean(text) && !PLACEHOLDERS.includes(text)
}
