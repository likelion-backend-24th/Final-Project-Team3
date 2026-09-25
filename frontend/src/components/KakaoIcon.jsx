// 카카오 로그인 버튼용 말풍선 아이콘. currentColor를 써서 버튼 텍스트 색(검정)을 그대로 따라간다.
export default function KakaoIcon({ size = 18, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className} xmlns="http://www.w3.org/2000/svg">
      <path
        d="M12 3.5C6.753 3.5 2.5 6.847 2.5 10.972c0 2.633 1.744 4.946 4.375 6.276-.192.706-.696 2.573-.797 2.973-.125.494.181.487.382.354.157-.103 2.5-1.7 3.514-2.393.664.097 1.348.148 2.026.148 5.247 0 9.5-3.347 9.5-7.358S17.247 3.5 12 3.5z"
        fill="currentColor"
      />
    </svg>
  )
}
