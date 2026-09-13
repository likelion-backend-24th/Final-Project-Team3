// 회원가입/마이페이지에서 공통으로 쓰는 연령대·직무 선택지.
// 값(value)은 백엔드 AgeGroup/Job enum과 정확히 일치해야 한다(member-service).
export const AGE_GROUPS = [
  { value: 'TEENS', label: '10대' },
  { value: 'TWENTIES', label: '20대' },
  { value: 'THIRTIES', label: '30대' },
  { value: 'FORTIES', label: '40대' },
  { value: 'FIFTIES_OR_OLDER', label: '50대 이상' },
]

export const JOBS = [
  { value: 'DEVELOPER', label: '개발' },
  { value: 'PLANNER_PM', label: '기획·PM' },
  { value: 'DESIGNER', label: '디자인' },
  { value: 'DATA_AI', label: '데이터·AI' },
  { value: 'MARKETING_SALES', label: '마케팅·영업' },
  { value: 'STUDENT', label: '학생' },
  { value: 'OTHER', label: '기타' },
]
