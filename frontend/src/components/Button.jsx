const variants = {
  primary: 'bg-primary hover:bg-primary-hover text-white',
  secondary: 'bg-surface2 hover:bg-border text-text border border-border',
  ghost: 'bg-transparent hover:bg-surface2 text-text-muted',
  danger: 'bg-transparent hover:bg-surface2 text-danger',
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
      className={`px-5 py-3 rounded-lg font-medium text-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${variants[variant]} ${className}`}
      {...props}
    >
      {loading ? '처리 중...' : children}
    </button>
  )
}
