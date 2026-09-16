import { useState } from 'react'
import { updatePgCredential } from '../../api/admin'
import { ApiError } from '../../api/client'
import TextField from '../../components/TextField'
import Button from '../../components/Button'

export default function SystemSettings() {
  const [form, setForm] = useState({ provider: 'tosspayments', apiKey: '', secretKey: '' })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(null)

  const setField = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    setSaved(null)
    try {
      const res = await updatePgCredential(form)
      setSaved(res.data)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-semibold text-text mb-1">시스템 설정</h1>
      <p className="text-text-muted mb-8">PG 연동</p>

      <form onSubmit={submit} className="bg-surface border border-border rounded-xl p-6 space-y-4">
        <h2 className="text-sm font-medium text-text">PG 연동</h2>
        <TextField label="PG사" value={form.provider} onChange={setField('provider')} required />
        <TextField label="API Key" value={form.apiKey} onChange={setField('apiKey')} required />
        <TextField
          label="Secret Key"
          type="password"
          value={form.secretKey}
          onChange={setField('secretKey')}
          required
        />

        {saved && (
          <p className="text-sm text-success bg-success/10 rounded-lg px-3 py-2">
            저장 완료 · Secret Key {saved.secretKey} · {new Date(saved.updatedAt).toLocaleString('ko-KR')}
          </p>
        )}
        {error && <p className="text-sm text-danger">{error}</p>}

        <Button type="submit" loading={saving}>
          저장
        </Button>
      </form>

      <p className="text-xs text-text-faint mt-4">
        Webhook URL·플랫폼 수수료율·알림 설정(대기열/결제/체크인)은 아직 백엔드 계약에 없어서 표시하지
        않아요. 저장된 PG 연동 상태를 조회하는 API도 없어서, 현재 값 표시 없이 새로 입력·저장만 가능해요.
      </p>
    </div>
  )
}
