import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Building2, CheckCircle2 } from 'lucide-react'
import TextField from '../components/TextField'
import Button from '../components/Button'
import { sendEmailCode, verifyEmailCode, signupOrganizer } from '../api/auth'
import { ApiError } from '../api/client'

// 이메일 인증이 signupOrganizer의 선행 조건이라(MEMBER_EMAIL_NOT_VERIFIED),
// 참가자 가입(SignupParticipant)과 동일하게 같은 카드 안에서 단계만 전환한다:
// email 입력 → 인증코드 발송 → 코드 확인 → 나머지 정보 입력 → 가입.
export default function SignupOrganizer() {
  const navigate = useNavigate()
  const [step, setStep] = useState('email') // 'email' | 'code' | 'verified'
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [form, setForm] = useState({
    organizationName: '',
    name: '',
    businessNo: '',
    password: '',
  })
  const [error, setError] = useState('')

  const [sendLoading, setSendLoading] = useState(false)
  const [verifyLoading, setVerifyLoading] = useState(false)
  const [loading, setLoading] = useState(false)

  const update = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

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
      await signupOrganizer({ ...form, email })
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
        <p className="text-sm text-text-muted mb-6">이메일 인증과 사업자등록번호 인증 후 즉시 대시보드를 이용할 수 있습니다</p>

        <form
          onSubmit={step === 'email' ? sendCode : step === 'code' ? verifyCode : submit}
          className="bg-surface border border-border rounded-xl p-6 space-y-4"
        >
          <div className="flex items-end gap-2">
            <div className="flex-1">
              <TextField
                label="담당자 이메일"
                type="email"
                placeholder="admin@org.com"
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
              <TextField
                label="단체명 / 법인명"
                placeholder="예: 한국 개발자 협회"
                value={form.organizationName}
                onChange={update('organizationName')}
                required
              />
              <TextField label="담당자 이름" placeholder="홍길동" value={form.name} onChange={update('name')} required />
              <TextField
                label="사업자등록번호"
                placeholder="10자리 숫자, - 없이"
                value={form.businessNo}
                onChange={(e) => setForm((f) => ({ ...f, businessNo: e.target.value.replace(/\D/g, '').slice(0, 10) }))}
                maxLength={10}
                inputMode="numeric"
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
            </>
          )}

          {error && <p className="text-sm text-danger">{error}</p>}

          {step === 'verified' && (
            <Button type="submit" loading={loading} className="w-full">
              가입하기
            </Button>
          )}
        </form>
      </div>
    </div>
  )
}
