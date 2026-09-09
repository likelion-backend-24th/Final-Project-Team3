import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { User, CheckCircle2 } from 'lucide-react'
import TextField from '../components/TextField'
import Button from '../components/Button'
import { sendEmailCode, verifyEmailCode, signupParticipant } from '../api/auth'
import { ApiError } from '../api/client'

// 이메일 인증(#87)이 signup의 선행 조건이라, 같은 카드 안에서 단계만 전환한다:
// email 입력 → 인증코드 발송 → 코드 확인 → 이름/비밀번호 입력 → 가입.
export default function SignupParticipant() {
  const navigate = useNavigate()
  const [step, setStep] = useState('email') // 'email' | 'code' | 'verified'
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')

  const [sendLoading, setSendLoading] = useState(false)
  const [verifyLoading, setVerifyLoading] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const sendCode = async (e) => {
    e.preventDefault()
    setError('')
    setSendLoading(true)
    try {
      await sendEmailCode(email)
      setStep('code')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '인증코드 발송에 실패했습니다.')
    } finally {
      setSendLoading(false)
    }
  }

  const verifyCode = async (e) => {
    e.preventDefault()
    setError('')
    setVerifyLoading(true)
    try {
      await verifyEmailCode(email, code)
      setStep('verified')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '인증코드 확인에 실패했습니다.')
    } finally {
      setVerifyLoading(false)
    }
  }

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await signupParticipant({ email, password, name })
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

        <form onSubmit={step === 'email' ? sendCode : verifyCode} className="mt-6 space-y-4">
          <div className="flex items-end gap-2">
            <div className="flex-1">
              <TextField
                label="이메일"
                type="email"
                placeholder="me@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={step !== 'email'}
                required
              />
            </div>
            {step !== 'verified' && (
              <Button
                type={step === 'email' ? 'submit' : 'button'}
                variant="secondary"
                loading={sendLoading}
                onClick={step === 'code' ? sendCode : undefined}
                className="shrink-0 whitespace-nowrap"
              >
                {step === 'email' ? '인증코드 발송' : '재발송'}
              </Button>
            )}
            {step === 'verified' && (
              <span className="shrink-0 inline-flex items-center gap-1 text-sm text-success pb-3">
                <CheckCircle2 size={16} /> 인증 완료
              </span>
            )}
          </div>

          {step === 'code' && (
            <div className="flex items-end gap-2">
              <div className="flex-1">
                <TextField
                  label="인증코드"
                  placeholder="6자리 숫자"
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  inputMode="numeric"
                  maxLength={6}
                  required
                />
              </div>
              <Button type="submit" loading={verifyLoading} className="shrink-0">
                확인
              </Button>
            </div>
          )}

          {step === 'verified' && (
            <>
              <TextField label="이름" placeholder="홍길동" value={name} onChange={(e) => setName(e.target.value)} required />
              <TextField
                label="비밀번호"
                type="password"
                placeholder="8자 이상"
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </>
          )}

          {error && <p className="text-sm text-danger">{error}</p>}

          {step === 'verified' && (
            <Button type="button" onClick={submit} loading={loading} className="w-full">
              가입하기
            </Button>
          )}
        </form>

        <p className="text-center text-sm text-text-muted mt-4">
          이미 계정이 있으신가요? <Link to="/login" className="text-accent">로그인</Link>
        </p>
      </div>
    </div>
  )
}
