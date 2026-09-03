import { Link } from 'react-router-dom'
import { Construction } from 'lucide-react'

export default function ComingSoon({ title }) {
  return (
    <div className="max-w-6xl mx-auto px-6 py-24 text-center">
      <Construction className="mx-auto text-text-faint mb-4" size={40} />
      <h1 className="text-xl font-semibold text-text mb-2">{title}</h1>
      <p className="text-text-muted mb-6">해당 기능을 지원하는 API가 아직 준비되지 않았어요.</p>
      <Link to="/organizer" className="text-accent text-sm">
        대시보드로 돌아가기
      </Link>
    </div>
  )
}
