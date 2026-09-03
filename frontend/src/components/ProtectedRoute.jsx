import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// role을 지정하면 해당 role만 통과, 아니면 로그인 여부만 확인한다.
export default function ProtectedRoute({ children, role }) {
  const { status, claims } = useAuth()

  if (status === 'loading') {
    return <div className="max-w-6xl mx-auto px-6 py-20 text-center text-text-muted">불러오는 중...</div>
  }
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace />
  }
  if (role && claims?.role !== role) {
    return <Navigate to="/" replace />
  }
  return children
}
