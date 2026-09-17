import { useCallback, useEffect, useRef, useState } from 'react'
import jsQR from 'jsqr'
import { Check, X, AlertTriangle, ScanLine } from 'lucide-react'
import { scanQrTicket } from '../../api/reservations'
import { ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'

// 백엔드 POST /api/qr-tickets/{code}/scan 응답은 {code, used, usedAt}뿐이다.
// 참가자 이름·세션 정보를 함께 주는 계약이 없어서(QrTicketScanResponse 참고) 성공 카드에도 못 넣는다.
const STATES = [
  { key: 'scanning', label: '스캔 중' },
  { key: 'success', label: '입장 성공' },
  { key: 'used', label: '이미 사용된 QR' },
  { key: 'invalid', label: '유효하지 않은 QR' },
]

function formatTime(isoString) {
  if (!isoString) return ''
  return new Date(isoString).toLocaleString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

export default function CheckIn() {
  const [phase, setPhase] = useState('scanning')
  const [result, setResult] = useState(null)
  const [manualCode, setManualCode] = useState('')
  const [cameraError, setCameraError] = useState('')

  const videoRef = useRef(null)
  const canvasRef = useRef(document.createElement('canvas'))
  const streamRef = useRef(null)
  const rafRef = useRef(null)
  const processingRef = useRef(false)

  const submitCode = useCallback(async (code) => {
    if (!code || processingRef.current) return
    processingRef.current = true
    try {
      const res = await scanQrTicket(code)
      setResult({ code: res.data.code, usedAt: res.data.usedAt })
      setPhase('success')
    } catch (err) {
      if (err instanceof ApiError && err.code === 'QR_TICKET_ALREADY_USED') {
        setResult({ code })
        setPhase('used')
      } else {
        setResult({ code, message: err instanceof ApiError ? err.message : '인식되지 않는 QR입니다. 다시 시도하거나 데스크에 문의해 주세요.' })
        setPhase('invalid')
      }
    }
  }, [])

  const tick = useCallback(() => {
    const video = videoRef.current
    const canvas = canvasRef.current
    if (video && video.readyState === video.HAVE_ENOUGH_DATA) {
      canvas.width = video.videoWidth
      canvas.height = video.videoHeight
      const ctx = canvas.getContext('2d')
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
      const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
      const qr = jsQR(imageData.data, imageData.width, imageData.height)
      if (qr?.data) {
        submitCode(qr.data)
        return
      }
    }
    rafRef.current = requestAnimationFrame(tick)
  }, [submitCode])

  useEffect(() => {
    if (phase !== 'scanning') return undefined

    let cancelled = false
    processingRef.current = false
    setCameraError('')

    navigator.mediaDevices
      ?.getUserMedia({ video: { facingMode: 'environment' } })
      .then((stream) => {
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop())
          return
        }
        streamRef.current = stream
        if (videoRef.current) {
          videoRef.current.srcObject = stream
          videoRef.current.play()
        }
        rafRef.current = requestAnimationFrame(tick)
      })
      .catch(() => {
        if (!cancelled) setCameraError('카메라를 사용할 수 없어요. 아래에 QR 코드 값을 직접 입력해주세요.')
      })

    return () => {
      cancelled = true
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
      streamRef.current?.getTracks().forEach((t) => t.stop())
      streamRef.current = null
    }
  }, [phase, tick])

  const rescan = () => {
    setResult(null)
    setManualCode('')
    setPhase('scanning')
  }

  const submitManual = (e) => {
    e.preventDefault()
    if (!manualCode.trim()) return
    submitCode(manualCode.trim())
  }

  return (
    <div className="max-w-3xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-semibold text-text mb-1">QR 체크인</h1>
      <p className="text-text-muted mb-5">입장권을 스캔해 주세요</p>

      <div className="flex flex-wrap gap-2 mb-6">
        {STATES.map((s) => (
          <span
            key={s.key}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium border ${
              phase === s.key ? 'border-primary text-text bg-primary/10' : 'border-border text-text-faint'
            }`}
          >
            {s.label}
          </span>
        ))}
      </div>

      <div
        className={`relative rounded-xl overflow-hidden border h-96 flex items-center justify-center ${
          phase === 'success'
            ? 'border-success bg-success/10'
            : phase === 'used'
              ? 'border-warning bg-warning/10'
              : phase === 'invalid'
                ? 'border-danger bg-danger/10'
                : 'border-border bg-surface'
        }`}
      >
        {phase === 'scanning' && (
          <>
            <video ref={videoRef} className="absolute inset-0 w-full h-full object-cover" muted playsInline />
            <div className="absolute inset-8 border-2 border-primary rounded-lg pointer-events-none" />
            {cameraError && (
              <div className="relative bg-bg/90 text-center px-6 py-4 rounded-lg">
                <ScanLine className="mx-auto text-text-faint mb-2" size={28} />
                <p className="text-sm text-text-muted">{cameraError}</p>
              </div>
            )}
          </>
        )}

        {phase === 'success' && (
          <div className="text-center">
            <div className="w-16 h-16 rounded-full bg-success/20 text-success flex items-center justify-center mx-auto mb-4">
              <Check size={32} />
            </div>
            <p className="text-success font-medium text-lg">입장 확인됨</p>
          </div>
        )}

        {phase === 'used' && (
          <div className="text-center">
            <div className="w-16 h-16 rounded-full bg-warning/20 text-warning flex items-center justify-center mx-auto mb-4">
              <AlertTriangle size={32} />
            </div>
            <p className="text-warning font-medium text-lg">이미 사용된 티켓</p>
          </div>
        )}

        {phase === 'invalid' && (
          <div className="text-center">
            <div className="w-16 h-16 rounded-full bg-danger/20 text-danger flex items-center justify-center mx-auto mb-4">
              <X size={32} />
            </div>
            <p className="text-danger font-medium text-lg">유효하지 않은 티켓</p>
          </div>
        )}
      </div>

      {phase === 'success' && result && (
        <div className="bg-surface border border-border rounded-xl p-5 mt-4">
          <p className="text-xs text-text-faint">티켓 코드</p>
          <p className="text-text font-mono text-sm mb-2">{result.code}</p>
          <p className="text-xs text-text-faint">입장 처리 시각 {formatTime(result.usedAt)}</p>
        </div>
      )}

      {phase === 'used' && (
        <p className="bg-warning/10 text-warning text-sm rounded-xl px-4 py-3 mt-4">
          이 티켓은 이미 사용 처리되었습니다.
        </p>
      )}

      {phase === 'invalid' && (
        <p className="bg-danger/10 text-danger text-sm rounded-xl px-4 py-3 mt-4">
          {result?.message ?? '인식되지 않는 QR입니다. 다시 시도하거나 데스크에 문의해 주세요.'}
        </p>
      )}

      {phase !== 'scanning' && (
        <Button variant="secondary" className="w-full mt-4" onClick={rescan}>
          다시 스캔
        </Button>
      )}

      {phase === 'scanning' && (
        <form onSubmit={submitManual} className="flex gap-2 mt-4">
          <TextField
            className="flex-1"
            placeholder="카메라 인식이 안 되면 QR 코드 값을 직접 입력하세요"
            value={manualCode}
            onChange={(e) => setManualCode(e.target.value)}
          />
          <Button type="submit">확인</Button>
        </form>
      )}
    </div>
  )
}
