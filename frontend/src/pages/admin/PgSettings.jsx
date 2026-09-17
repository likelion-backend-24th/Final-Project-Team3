import { useState } from 'react'
import { registerPgCredential } from '../../api/admin'
import { ApiError } from '../../api/client'
import Button from '../../components/Button'

const inputClass =
  'w-full bg-bg border border-border rounded-lg px-4 py-3 text-sm text-text placeholder:text-text-faint focus:outline-none focus:border-primary'

export default function PgSettings() {
  const [provider, setProvider] = useState('')
  const [apiKey, setApiKey] = useState('')
  const [secretKey, setSecretKey] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(null)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setSaved(null)
    setLoading(true)
    try {
      const res = await registerPgCredential({ provider: provider.trim(), apiKey: apiKey.trim(), secretKey: secretKey.trim() })
      setSaved(res.data)
      setApiKey('')
      setSecretKey('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'PG 키 등록에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-lg mx-auto px-6 py-10">
      <h1 className="text-2xl font-semibold text-text">시스템 설정</h1>
      <p className="text-sm text-text-muted mt-1">PG(결제대행사) 연동 키를 등록합니다.</p>

      <form onSubmit={submit} className="mt-8 space-y-4 bg-surface border border-border rounded-xl p-6">
        <div>
          <label className="text-sm text-text-muted block mb-1.5">PG사 (provider)</label>
          <input
            className={inputClass}
            placeholder="예: TOSS"
            value={provider}
            onChange={(e) => setProvider(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="text-sm text-text-muted block mb-1.5">API Key</label>
          <input
            className={inputClass}
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="text-sm text-text-muted block mb-1.5">Secret Key</label>
          <input
            className={inputClass}
            type="password"
            value={secretKey}
            onChange={(e) => setSecretKey(e.target.value)}
            required
          />
          <p className="text-xs text-text-faint mt-1">저장 후에는 암호화되어 뒤 4자리만 확인할 수 있어요.</p>
        </div>

        {error && <p className="text-sm text-danger">{error}</p>}

        {saved && (
          <div className="bg-surface2 rounded-lg p-4 text-sm">
            <p className="text-text">
              <span className="text-text-muted">{saved.provider}</span> 키 저장 완료
            </p>
            <p className="text-text-faint mt-1 font-mono">{saved.secretKey}</p>
          </div>
        )}

        <Button type="submit" loading={loading} className="w-full">
          저장
        </Button>
      </form>
    </div>
  )
}
