import { useRef, useState } from 'react'
import { Image as ImageIcon } from 'lucide-react'
import { uploadDescriptionImage } from '../api/conferences'
import { ApiError } from '../api/client'

// 백엔드(FileStorageService)와 같은 제한: 확장자 png/jpg/jpeg, 파일당 10MB(multipart 한도).
const IMAGE_EXTENSIONS = ['png', 'jpg', 'jpeg']
const MAX_IMAGE_BYTES = 10 * 1024 * 1024
// AI 요약이 대표 배너 + 소개글 이미지 합쳐 최대 2장만 분석하므로, 소개글 쪽 이미지는 1장으로 제한한다
// (백엔드도 저장 시 같은 규칙을 다시 검증함 - ConferenceContentSummaryService.validateImageLimit).
const MAX_DESCRIPTION_IMAGES = 1

// DescriptionText.jsx가 렌더링에 쓰는 것과 같은 패턴 - 여기서도 똑같이 세야 렌더링·업로드 제한이 어긋나지 않는다.
function countImages(text) {
  return (text.match(/!\[[^\]]*\]\(\S+\)/g) ?? []).length
}

// 소개글 textarea + "이미지 삽입" 버튼. 고른 이미지를 바로 업로드해서 받은 URL을 커서 위치에
// `![alt](url)` 마크다운으로 꽂아넣는다 - 소개글 자체는 여전히 TEXT 컬럼 하나라, 이 문법을
// 실제 이미지로 보여주는 건 렌더링 쪽(DescriptionText)의 몫이다.
export default function DescriptionEditor({ value, onChange, rows = 8, placeholder, className }) {
  const textareaRef = useRef(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')

  const insertAtCursor = (snippet) => {
    const textarea = textareaRef.current
    if (!textarea) {
      onChange(value + snippet)
      return
    }
    const start = textarea.selectionStart ?? value.length
    const end = textarea.selectionEnd ?? value.length
    onChange(value.slice(0, start) + snippet + value.slice(end))
    // 삽입한 문법 뒤로 커서를 옮겨야 이어서 타이핑할 수 있다 (onChange가 리렌더링을 트리거하므로 다음 프레임에 적용)
    requestAnimationFrame(() => {
      textarea.focus()
      textarea.setSelectionRange(start + snippet.length, start + snippet.length)
    })
  }

  const imageCount = countImages(value)
  const limitReached = imageCount >= MAX_DESCRIPTION_IMAGES

  const onPickImage = async (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    if (limitReached) {
      setError(`이미지는 소개글당 최대 ${MAX_DESCRIPTION_IMAGES}장까지만 넣을 수 있어요.`)
      return
    }
    const ext = file.name.split('.').pop().toLowerCase()
    if (!IMAGE_EXTENSIONS.includes(ext)) {
      setError('이미지는 PNG, JPG만 첨부할 수 있어요.')
      return
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setError('이미지는 10MB 이하만 첨부할 수 있어요.')
      return
    }
    setError('')
    setUploading(true)
    try {
      const res = await uploadDescriptionImage(file)
      insertAtCursor(`\n![${file.name}](${res.data.imageUrl})\n`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '이미지 업로드에 실패했습니다.')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div>
      <textarea
        ref={textareaRef}
        rows={rows}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className={className}
      />
      {limitReached ? (
        <p className="mt-2 text-xs text-text-faint">이미지는 소개글당 최대 {MAX_DESCRIPTION_IMAGES}장까지만 넣을 수 있어요.</p>
      ) : (
        <label className="mt-2 inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text cursor-pointer">
          <ImageIcon size={14} />
          {uploading ? '업로드 중...' : '이미지 삽입'}
          <input
            type="file"
            accept=".png,.jpg,.jpeg"
            onChange={onPickImage}
            disabled={uploading}
            className="hidden"
          />
        </label>
      )}
      {error && <p className="text-xs text-danger mt-1">{error}</p>}
    </div>
  )
}
