import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Header from './components/Header'
import ProtectedRoute from './components/ProtectedRoute'

import SignupChoice from './pages/SignupChoice'
import SignupParticipant from './pages/SignupParticipant'
import SignupOrganizer from './pages/SignupOrganizer'
import Login from './pages/Login'
import Home from './pages/Home'
import ConferenceDetail from './pages/ConferenceDetail'
import SessionApply from './pages/SessionApply'
import ReservationComplete from './pages/ReservationComplete'
import QueueStatus from './pages/QueueStatus'
import MyPage from './pages/MyPage'

import OrganizerDashboard from './pages/organizer/Dashboard'
import ConferenceCreate from './pages/organizer/ConferenceCreate'
import ComingSoon from './pages/organizer/ComingSoon'

function Layout() {
  return (
    <div className="min-h-screen bg-bg">
      <Header />
      <Outlet />
    </div>
  )
}

// 주최자는 "홈"을 눌러도 컨퍼런스 목록이 아니라 자기 대시보드로 가야 한다.
function HomeOrDashboard() {
  const { claims } = useAuth()
  if (claims?.role === 'ORGANIZER') return <Navigate to="/organizer" replace />
  return <Home />
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<HomeOrDashboard />} />
            <Route path="/conferences" element={<Home />} />
            <Route path="/conferences/:id" element={<ConferenceDetail />} />

            <Route path="/signup" element={<SignupChoice />} />
            <Route path="/signup/participant" element={<SignupParticipant />} />
            <Route path="/signup/organizer" element={<SignupOrganizer />} />
            <Route path="/login" element={<Login />} />

            <Route
              path="/conferences/:id/sessions/:sessionId/apply"
              element={
                <ProtectedRoute role="MEMBER">
                  <SessionApply />
                </ProtectedRoute>
              }
            />
            <Route
              path="/reservations/:id/complete"
              element={
                <ProtectedRoute role="MEMBER">
                  <ReservationComplete />
                </ProtectedRoute>
              }
            />
            <Route
              path="/reservations/:id/queue"
              element={
                <ProtectedRoute role="MEMBER">
                  <QueueStatus />
                </ProtectedRoute>
              }
            />
            <Route
              path="/my"
              element={
                <ProtectedRoute role="MEMBER">
                  <MyPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/organizer"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <OrganizerDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/conferences/new"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <ConferenceCreate />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/operations"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <ComingSoon title="운영 현황" />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/checkin"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <ComingSoon title="QR 체크인" />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/settlements"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <ComingSoon title="정산 내역" />
                </ProtectedRoute>
              }
            />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
