/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        bg: '#0a0a12',
        surface: '#12131c',
        surface2: '#181a26',
        border: '#242637',
        primary: {
          DEFAULT: '#4F6EF7',
          hover: '#3E56C1',
        },
        accent: '#818cf8',
        text: {
          DEFAULT: '#f2f2f7',
          muted: '#9295a8',
          faint: '#5c5f72',
        },
        success: '#22c55e',
        warning: '#f5a623',
        danger: '#f87171',
      },
      fontFamily: {
        sans: ['Pretendard', 'Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
