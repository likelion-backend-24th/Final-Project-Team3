import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Building2 } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { getProfile, withdrawMember } from '../../api/auth'
import { ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'

// 주최자 전용 "본인 계정" 화면. 참석자 마이페이지 같은 프로필 수정 기능은 범위 밖이고,
// 지금은 계정 정보 읽기전용 표시 + 탈퇴 기능만 제공한다(#251).
export default function OrganizerSettings() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')

  const [withdrawOpen, setWithdrawOpen] = useState(false)
  const [withdrawPassword, setWithdrawPassword] = useState('')
  const [withdrawing, setWithdrawing] = useState(false)
  const [withdrawError, setWithdrawError] = useState('')

  useEffect(() => {
    getProfile()
      .then((res) => setProfile(res.data))
      .catch((err) => setError(err instanceof ApiError ? err.message : '정보를 불러오지 못했습니다.'))
  }, [])

  // 주최자는 소셜 로그인 대상에서 제외돼있어 항상 비밀번호 계정이다 — 소셜 재인증 분기가 필요 없다.
  const submitWithdraw = async (e) => {
    e.preventDefault()
    if (!window.confirm('정말 탈퇴하시겠어요? 이 작업은 되돌릴 수 없어요.')) return
    setWithdrawError('')
    setWithdrawing(true)
    try {
      await withdrawMember({ password: withdrawPassword })
      await logout()
      navigate('/login', { state: { withdrawDone: true } })
    } catch (err) {
      setWithdrawError(err instanceof ApiError ? err.message : '탈퇴에 실패했습니다.')
      setWithdrawing(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-10">
      <div className="max-w-lg mx-auto">
        <h1 className="text-lg font-semibold text-text mb-6">계정 정보</h1>

        <div className="bg-surface border border-border rounded-xl p-5 mb-8">
          <div className="flex items-center gap-2 mb-4">
            <Building2 size={16} className="text-text-muted" />
            <h2 className="text-sm font-semibold text-text">사업자 정보</h2>
          </div>

          {error && <p className="text-sm text-danger">{error}</p>}

          {profile && (
            <dl className="space-y-3 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-text-muted shrink-0">이메일</dt>
                <dd className="text-text text-right">{profile.email}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-text-muted shrink-0">기관명</dt>
                <dd className="text-text text-right">{profile.organizationName}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-text-muted shrink-0">사업자등록번호</dt>
                <dd className="text-text text-right">{profile.businessNo}</dd>
              </div>
            </dl>
          )}
        </div>

        <div className="bg-surface border border-border rounded-xl p-5">
          <h2 className="text-sm font-semibold text-text mb-1">회원 탈퇴</h2>
          <p className="text-xs text-text-muted mb-4">
            진행 중인(승인대기·승인) 컨퍼런스가 있으면 탈퇴할 수 없어요. 탈퇴하면 계정 정보가 삭제되고
            다시 로그인할 수 없게 돼요. 이 작업은 되돌릴 수 없어요.
          </p>

          {!withdrawOpen ? (
            <Button variant="secondary" onClick={() => setWithdrawOpen(true)}>
              탈퇴하기
            </Button>
          ) : (
            <form onSubmit={submitWithdraw} className="space-y-3">
              <TextField
                label="현재 비밀번호"
                type="password"
                placeholder="본인 확인을 위해 입력해주세요"
                value={withdrawPassword}
                onChange={(e) => setWithdrawPassword(e.target.value)}
                required
              />
              {withdrawError && <p className="text-sm text-danger">{withdrawError}</p>}
              <div className="flex gap-2">
                <Button type="submit" variant="danger" loading={withdrawing}>
                  탈퇴 확정
                </Button>
                <Button type="button" variant="secondary" onClick={() => setWithdrawOpen(false)}>
                  취소
                </Button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}
