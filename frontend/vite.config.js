import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      // Gateway (Spring Cloud Gateway MVC)가 8080에서 각 서비스로 라우팅한다.
      // 개발 서버가 /api 요청을 gateway로 프록시해서 CORS 설정 없이 쿠키 기반 인증을 그대로 쓸 수 있게 한다.
      '/api': {
        // 로컬에서 8080이 다른 프로세스에 이미 점유돼 있으면 GATEWAY_URL로 override 가능
        // (예: GATEWAY_URL=http://localhost:8090 npm run dev)
        target: process.env.GATEWAY_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
