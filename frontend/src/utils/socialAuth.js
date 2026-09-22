// Google Identity Services / Kakao SDK 로더 + 호출 래퍼.
// 두 서비스 다 클라이언트 키가 .env에 없으면(로컬에서 콘솔 앱을 아직 안 만들었으면) "설정 안 됨" 취급하고,
// 호출부(Login.jsx)가 이 경우 mock 입력창으로 대체한다 — 백엔드 SOCIAL_MODE=mock과 짝을 맞춤.
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID
const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY

export const isGoogleConfigured = Boolean(GOOGLE_CLIENT_ID)
export const isKakaoConfigured = Boolean(KAKAO_JS_KEY)

function loadScript(src) {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) return resolve()
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.defer = true
    script.onload = resolve
    script.onerror = reject
    document.head.appendChild(script)
  })
}

// container(div)에 Google 공식 버튼을 그려 넣는다. Google 가이드라인상 커스텀 버튼에 클릭 이벤트를
// 붙이는 방식은 권장되지 않아서, 렌더된 버튼을 그대로 쓰고 완료되면 idToken을 onToken으로 넘긴다.
export async function renderGoogleButton(container, onToken) {
  if (!isGoogleConfigured || !container) return
  await loadScript('https://accounts.google.com/gsi/client')
  window.google.accounts.id.initialize({
    client_id: GOOGLE_CLIENT_ID,
    callback: (response) => onToken(response.credential),
  })
  window.google.accounts.id.renderButton(container, {
    theme: 'outline',
    size: 'large',
    width: 320,
    text: 'continue_with',
  })
}

export async function kakaoLogin(onToken, onError) {
  if (!isKakaoConfigured) return
  await loadScript('https://developers.kakao.com/sdk/js/kakao.js')
  if (!window.Kakao.isInitialized()) {
    window.Kakao.init(KAKAO_JS_KEY)
  }
  window.Kakao.Auth.login({
    success: (authObj) => onToken(authObj.access_token),
    fail: (err) => onError?.(err),
  })
}
