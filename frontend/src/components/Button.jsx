const variants = {
  primary: 'bg-primary hover:bg-primary-hover text-white',
  secondary: 'bg-surface2 hover:bg-border text-text border border-border',
  ghost: 'bg-transparent hover:bg-surface2 text-text-muted',
  danger: 'bg-transparent hover:bg-surface2 text-danger',
  // 카카오 브랜드 가이드 색상(#FEE500) — 소셜 로그인 버튼 전용
  kakao: 'bg-[#FEE500] hover:bg-[#FDD800] text-black',
  // Google 로그인 버튼 — 공식 렌더 버튼 대신 커스텀 버튼을 쓰므로 outline 테마와 비슷하게 맞춤
  google: 'bg-white hover:bg-gray-50 text-gray-700 border border-gray-300',
}

export default function Button({
  children,
  variant = 'primary',
  className = '',
  disabled = false,
  loading = false,
  ...props
}) {
  return (
    <button
      disabled={disabled || loading}
      className={`px-5 py-3 rounded-lg font-medium text-sm whitespace-nowrap transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${variants[variant]} ${className}`}
      {...props}
    >
      {loading ? '처리 중...' : children}
    </button>
  )
}
