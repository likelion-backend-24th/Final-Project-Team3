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

// 공식 렌더 버튼 대신 커스텀 버튼(Button variant="google")을 써서 카카오 버튼과 폰트·둥글기·너비를
// 맞춘다. One Tap(google.accounts.id.prompt())은 "이미 구글에 로그인된 세션"이 있어야만 뜨고
// 없으면(시크릿창 등) 버튼을 눌러도 아무 반응이 없어서, 카카오처럼 클릭하면 항상 로그인 팝업이
// 뜨는 OAuth2 방식(initTokenClient)으로 바꿨다. 토큰 클라이언트는 한 번만 만들어서 재사용한다.
let googleTokenClient = null

export async function googleLogin(onToken) {
  if (!isGoogleConfigured) return
  await loadScript('https://accounts.google.com/gsi/client')
  if (!googleTokenClient) {
    googleTokenClient = window.google.accounts.oauth2.initTokenClient({
      client_id: GOOGLE_CLIENT_ID,
      scope: 'openid email profile',
      callback: (response) => {
        if (response?.access_token) onToken(response.access_token)
      },
    })
  }
  googleTokenClient.requestAccessToken()
}

// Kakao JS SDK는 v2부터 팝업 로그인(Auth.login)을 지원하지 않는다 — 반드시 Auth.authorize()로
// 인가 코드를 받아 페이지 전체를 리다이렉트해야 한다(SDK URL도 v1 시절과 다름: t1.kakaocdn.net).
const KAKAO_SDK_URL = 'https://t1.kakaocdn.net/kakao_js_sdk/2.8.3/kakao.min.js'

export function kakaoRedirectUri() {
  return `${window.location.origin}/auth/kakao/callback`
}

// 인가 코드는 1회용이라, authorize() 왕복 사이에 로그인 의도(로그인/연동)와 최초가입 시
// 필요한 연령대·직무를 state에 실어 보낸다 — "추가 정보 필요" 재시도는 새 코드로 다시 authorize()를 호출해야 한다.
export function encodeKakaoState(data) {
  return btoa(encodeURIComponent(JSON.stringify(data)))
}

export function decodeKakaoState(state) {
  try {
    return JSON.parse(decodeURIComponent(atob(state)))
  } catch {
    return null
  }
}

export async function kakaoAuthorize(state) {
  if (!isKakaoConfigured) return
  await loadScript(KAKAO_SDK_URL)
  if (!window.Kakao.isInitialized()) {
    window.Kakao.init(KAKAO_JS_KEY)
  }
  window.Kakao.Auth.authorize({
    redirectUri: kakaoRedirectUri(),
    state: encodeKakaoState(state),
  })
}
