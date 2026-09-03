import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import TextField from '../components/TextField'
import Button from '../components/Button'
import { useAuth } from '../context/AuthContext'
import { ApiError } from '../api/client'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const claims = await login(email, password)
      navigate(claims?.role === 'ORGANIZER' ? '/organizer' : '/conferences')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-16">
      <div className="max-w-md mx-auto">
        <div className="flex bg-surface2 rounded-lg p-1 mb-6">
          <span className="flex-1 text-center py-2 rounded-md bg-surface text-text text-sm font-medium">로그인</span>
          <Link
            to="/signup"
            className="flex-1 text-center py-2 rounded-md text-text-muted text-sm font-medium hover:text-text"
          >
            회원가입
          </Link>
        </div>

        <div className="bg-surface border border-border rounded-xl p-6">
          <h1 className="text-lg font-semibold text-text mb-6">로그인</h1>

          {location.state?.justSignedUp && (
            <p className="mb-4 text-sm text-success bg-success/10 rounded-lg px-3 py-2">
              회원가입이 완료됐어요. 로그인해주세요.
            </p>
          )}

          <form onSubmit={submit} className="space-y-4">
            <TextField
              label="이메일"
              type="email"
              placeholder="me@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <TextField
              label="비밀번호"
              type="password"
              placeholder="********"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            {error && <p className="text-sm text-danger">{error}</p>}
            <Button type="submit" loading={loading} className="w-full">
              로그인
            </Button>
          </form>

          <p className="text-center text-sm text-text-muted mt-4">
            계정이 없으신가요? <Link to="/signup" className="text-accent">회원가입</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
