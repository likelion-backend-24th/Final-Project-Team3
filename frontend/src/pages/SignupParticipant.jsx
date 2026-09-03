import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { User } from 'lucide-react'
import TextField from '../components/TextField'
import Button from '../components/Button'
import { signupParticipant } from '../api/auth'
import { ApiError } from '../api/client'

export default function SignupParticipant() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const update = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await signupParticipant(form)
      navigate('/login', { state: { justSignedUp: true } })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '회원가입에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-16">
      <div className="max-w-md mx-auto">
        <Link to="/signup" className="text-sm text-text-muted hover:text-text">‹ 가입 유형 선택</Link>
      </div>

      <div className="max-w-md mx-auto mt-6 bg-surface border border-border rounded-xl p-6">
        <div className="flex items-center gap-2 mb-1">
          <span className="w-7 h-7 rounded-md bg-primary/20 text-primary flex items-center justify-center">
            <User size={16} />
          </span>
          <h1 className="text-lg font-semibold text-text">참가자 회원가입</h1>
        </div>

        <form onSubmit={submit} className="mt-6 space-y-4">
          <TextField label="이름" placeholder="홍길동" value={form.name} onChange={update('name')} required />
          <TextField
            label="이메일"
            type="email"
            placeholder="me@example.com"
            value={form.email}
            onChange={update('email')}
            required
          />
          <TextField
            label="비밀번호"
            type="password"
            placeholder="8자 이상"
            minLength={8}
            value={form.password}
            onChange={update('password')}
            required
          />
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" loading={loading} className="w-full">
            가입하기
          </Button>
        </form>

        <p className="text-center text-sm text-text-muted mt-4">
          이미 계정이 있으신가요? <Link to="/login" className="text-accent">로그인</Link>
        </p>
      </div>
    </div>
  )
}
