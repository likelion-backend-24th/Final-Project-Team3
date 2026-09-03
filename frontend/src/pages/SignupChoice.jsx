import { Link } from 'react-router-dom'
import { User, Building2, ChevronRight } from 'lucide-react'

const options = [
  {
    to: '/signup/participant',
    icon: User,
    iconClass: 'bg-primary/20 text-primary',
    title: '참가자로 가입',
    desc: '컨퍼런스 탐색, 세션 신청',
  },
  {
    to: '/signup/organizer',
    icon: Building2,
    iconClass: 'bg-warning/20 text-warning',
    title: '주최자로 시작하기',
    desc: '컨퍼런스 등록·운영',
  },
]

export default function SignupChoice() {
  return (
    <div className="max-w-6xl mx-auto px-6 py-16 text-center">
      <h1 className="text-2xl font-semibold text-text mb-2">회원가입</h1>
      <p className="text-text-muted mb-10">어떤 목적으로 가입하시나요?</p>

      <div className="flex flex-wrap justify-center gap-5 mb-8">
        {options.map(({ to, icon: Icon, iconClass, title, desc }) => (
          <Link
            key={to}
            to={to}
            className="w-64 text-left bg-surface border border-border rounded-xl p-6 hover:border-primary transition-colors"
          >
            <div className={`w-10 h-10 rounded-lg flex items-center justify-center mb-4 ${iconClass}`}>
              <Icon size={20} />
            </div>
            <h2 className="text-text font-medium mb-1">{title}</h2>
            <p className="text-sm text-text-muted mb-4">{desc}</p>
            <span className="inline-flex items-center gap-1 text-sm text-accent">
              즉시 사용 가능 <ChevronRight size={14} />
            </span>
          </Link>
        ))}
      </div>

      <p className="text-sm text-text-muted">
        이미 계정이 있으신가요? <Link to="/login" className="text-accent">로그인</Link>
      </p>
    </div>
  )
}
