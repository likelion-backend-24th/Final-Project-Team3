import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import TextField from '../components/TextField'
import Button from '../components/Button'
import { requestPasswordReset, confirmPasswordReset } from '../api/auth'
import { ApiError } from '../api/client'

export default function ForgotPassword() {
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [step, setStep] = useState('request') // 'request' | 'confirm'
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submitRequest = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await requestPasswordReset(email)
      setStep('confirm')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '요청에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const submitConfirm = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await confirmPasswordReset(email, code, newPassword)
      navigate('/login', { state: { passwordResetDone: true } })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '재설정에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  // 코드가 만료됐거나 재발송이 필요할 때 이메일 입력 단계로 되돌아가서 다시 요청하게 한다
  const backToRequest = () => {
    setStep('request')
    setCode('')
    setNewPassword('')
    setError('')
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-16">
      <div className="max-w-md mx-auto bg-surface border border-border rounded-xl p-6">
        <h1 className="text-lg font-semibold text-text mb-1">비밀번호 찾기</h1>

        {step === 'request' && (
          <>
            <p className="text-sm text-text-muted mb-6">가입하신 이메일로 인증코드를 보내드릴게요.</p>
            <form onSubmit={submitRequest} className="space-y-4">
              <TextField
                label="이메일"
                type="email"
                placeholder="me@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
              {error && <p className="text-sm text-danger">{error}</p>}
              <Button type="submit" loading={loading} className="w-full">
                인증코드 받기
              </Button>
            </form>
          </>
        )}

        {step === 'confirm' && (
          <>
            <p className="text-sm text-text-muted mb-6">
              <strong>{email}</strong>로 인증코드를 보냈어요. 코드와 새 비밀번호를 입력해주세요.
            </p>
            <form onSubmit={submitConfirm} className="space-y-4">
              <TextField
                label="인증코드"
                placeholder="6자리 숫자"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                required
              />
              <TextField
                label="새 비밀번호"
                type="password"
                placeholder="8자 이상"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
              {error && <p className="text-sm text-danger">{error}</p>}
              <Button type="submit" loading={loading} className="w-full">
                비밀번호 재설정
              </Button>
              <button
                type="button"
                onClick={backToRequest}
                className="w-full text-sm text-text-muted hover:text-text"
              >
                코드를 다시 받을까요?
              </button>
            </form>
          </>
        )}

        <p className="text-center text-sm text-text-muted mt-4">
          <Link to="/login" className="text-accent">로그인으로 돌아가기</Link>
        </p>
      </div>
    </div>
  )
}
