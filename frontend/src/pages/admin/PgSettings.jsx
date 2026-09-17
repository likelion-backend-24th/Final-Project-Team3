import { useState } from 'react'
import { registerPgCredential } from '../../api/admin'
import { ApiError } from '../../api/client'
import Button from '../../components/Button'

const inputClass =
  'w-full bg-bg border border-border rounded-lg px-4 py-3 text-sm text-text placeholder:text-text-faint focus:outline-none focus:border-primary'

export default function PgSettings() {
  const [provider, setProvider] = useState('PORTONE')
  const [storeId, setStoreId] = useState('')
  const [channelKey, setChannelKey] = useState('')
  const [apiSecret, setApiSecret] = useState('')
  const [webhookSecret, setWebhookSecret] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(null)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setSaved(null)
    setLoading(true)
    try {
      const res = await registerPgCredential({
        provider: provider.trim(),
        storeId: storeId.trim(),
        channelKey: channelKey.trim(),
        apiSecret: apiSecret.trim(),
        webhookSecret: webhookSecret.trim(),
      })
      setSaved(res.data)
      setApiSecret('')
      setWebhookSecret('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'PG 키 등록에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-lg mx-auto px-6 py-10">
      <h1 className="text-2xl font-semibold text-text">시스템 설정</h1>
      <p className="text-sm text-text-muted mt-1">포트원(PortOne) 결제 연동 정보를 등록합니다.</p>

      <form onSubmit={submit} className="mt-8 space-y-4 bg-surface border border-border rounded-xl p-6">
        <div>
          <label className="text-sm text-text-muted block mb-1.5">PG사 (provider)</label>
          <input
            className={inputClass}
            placeholder="예: PORTONE"
            value={provider}
            onChange={(e) => setProvider(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="text-sm text-text-muted block mb-1.5">Store ID</label>
          <input
            className={inputClass}
            placeholder="store-로 시작"
            value={storeId}
            onChange={(e) => setStoreId(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="text-sm text-text-muted block mb-1.5">채널 키 (channelKey)</label>
          <input
            className={inputClass}
            placeholder="예: 일반결제 채널키 (토스)"
            value={channelKey}
            onChange={(e) => setChannelKey(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="text-sm text-text-muted block mb-1.5">API Secret</label>
          <input
            className={inputClass}
            type="password"
            value={apiSecret}
            onChange={(e) => setApiSecret(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="text-sm text-text-muted block mb-1.5">웹훅 시크릿 (webhookSecret)</label>
          <input
            className={inputClass}
            type="password"
            value={webhookSecret}
            onChange={(e) => setWebhookSecret(e.target.value)}
            required
          />
          <p className="text-xs text-text-faint mt-1">저장 후에는 암호화되어 뒤 4자리만 확인할 수 있어요.</p>
        </div>

        {error && <p className="text-sm text-danger">{error}</p>}

        {saved && (
          <div className="bg-surface2 rounded-lg p-4 text-sm space-y-1">
            <p className="text-text">
              <span className="text-text-muted">{saved.provider}</span> 키 저장 완료
            </p>
            <p className="text-text-faint">Store ID: {saved.storeId}</p>
            <p className="text-text-faint">채널 키: {saved.channelKey}</p>
            <p className="text-text-faint font-mono">API Secret: {saved.apiSecret}</p>
            <p className="text-text-faint font-mono">웹훅 시크릿: {saved.webhookSecret}</p>
          </div>
        )}

        <Button type="submit" loading={loading} className="w-full">
          저장
        </Button>
      </form>
    </div>
  )
}
