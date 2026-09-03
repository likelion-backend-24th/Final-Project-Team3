import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Building2 } from 'lucide-react'
import TextField from '../components/TextField'
import Button from '../components/Button'
import { signupOrganizer } from '../api/auth'
import { ApiError } from '../api/client'

export default function SignupOrganizer() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    organizationName: '',
    name: '',
    email: '',
    businessNo: '',
    password: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const update = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await signupOrganizer(form)
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

      <div className="max-w-md mx-auto mt-6">
        <div className="flex items-center gap-2 mb-1">
          <span className="w-8 h-8 rounded-md bg-warning/20 text-warning flex items-center justify-center">
            <Building2 size={18} />
          </span>
          <h1 className="text-lg font-semibold text-text">주최자 회원가입</h1>
        </div>
        <p className="text-sm text-text-muted mb-6">사업자등록번호 인증 후 즉시 대시보드를 이용할 수 있습니다</p>

        <form onSubmit={submit} className="bg-surface border border-border rounded-xl p-6 space-y-4">
          <TextField
            label="단체명 / 법인명"
            placeholder="예: 한국 개발자 협회"
            value={form.organizationName}
            onChange={update('organizationName')}
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <TextField label="담당자 이름" placeholder="홍길동" value={form.name} onChange={update('name')} required />
            <TextField
              label="담당자 이메일"
              type="email"
              placeholder="admin@org.com"
              value={form.email}
              onChange={update('email')}
              required
            />
          </div>
          <TextField
            label="사업자등록번호"
            placeholder="10자리 숫자"
            value={form.businessNo}
            onChange={update('businessNo')}
            maxLength={12}
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
      </div>
    </div>
  )
}
