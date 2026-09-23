import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Header from './components/Header'
import ProtectedRoute from './components/ProtectedRoute'

import SignupChoice from './pages/SignupChoice'
import SignupParticipant from './pages/SignupParticipant'
import SignupOrganizer from './pages/SignupOrganizer'
import Login from './pages/Login'
import ForgotPassword from './pages/ForgotPassword'
import KakaoCallback from './pages/KakaoCallback'
import Home from './pages/Home'
import ConferenceDetail from './pages/ConferenceDetail'
import OrganizerProfile from './pages/OrganizerProfile'
import SessionApply from './pages/SessionApply'
import Payment from './pages/Payment'
import ReservationComplete from './pages/ReservationComplete'
import QueueStatus from './pages/QueueStatus'
import MyPage from './pages/MyPage'

import OrganizerDashboard from './pages/organizer/Dashboard'
import ConferenceCreate from './pages/organizer/ConferenceCreate'
import Applications from './pages/organizer/Applications'
import ConferenceSettings from './pages/organizer/ConferenceSettings'
import SessionManage from './pages/organizer/SessionManage'
import SessionCreate from './pages/organizer/SessionCreate'
import AttendeeSummary from './pages/organizer/AttendeeSummary'
import ReviewList from './pages/organizer/ReviewList'
import OperationStatus from './pages/organizer/OperationStatus'
import CheckIn from './pages/organizer/CheckIn'
import Settlements from './pages/organizer/Settlements'

import AdminApprovals from './pages/admin/Approvals'
import AdminPgSettings from './pages/admin/PgSettings'
import AdminSettlementDashboard from './pages/admin/SettlementDashboard'
import AdminUsers from './pages/admin/Users'
import AdminSidebar from './components/AdminSidebar'

function Layout() {
  return (
    <div className="min-h-screen bg-bg">
      <Header />
      <Outlet />
    </div>
  )
}

function AdminLayout() {
  return (
    <div className="min-h-screen bg-bg">
      <Header />
      <div className="flex">
        <AdminSidebar />
        <main className="flex-1 min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

// 주최자·전체관리자는 "홈"을 눌러도 컨퍼런스 목록이 아니라 각자의 관리 화면으로 가야 한다.
function HomeOrDashboard() {
  const { claims } = useAuth()
  if (claims?.role === 'ORGANIZER') return <Navigate to="/organizer" replace />
  if (claims?.role === 'ADMIN') return <Navigate to="/admin" replace />
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
            <Route path="/organizers/:organizerId" element={<OrganizerProfile />} />

            <Route path="/signup" element={<SignupChoice />} />
            <Route path="/signup/participant" element={<SignupParticipant />} />
            <Route path="/signup/organizer" element={<SignupOrganizer />} />
            <Route path="/login" element={<Login />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/auth/kakao/callback" element={<KakaoCallback />} />

            <Route
              path="/conferences/:id/sessions/:sessionId/apply"
              element={
                <ProtectedRoute role="MEMBER">
                  <SessionApply />
                </ProtectedRoute>
              }
            />
            <Route
              path="/reservations/:id/payment"
              element={
                <ProtectedRoute role="MEMBER">
                  <Payment />
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
              path="/organizer/applications"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <Applications />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/conferences/:id/settings"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <ConferenceSettings />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/conferences/:id/sessions"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <SessionManage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/conferences/:id/sessions/new"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <SessionCreate />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/conferences/:id/sessions/:sessionId/edit"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <SessionCreate />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/conferences/:id/attendee-summary"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <AttendeeSummary />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/conferences/:id/reviews"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <ReviewList />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/conferences/:id/operations"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <OperationStatus />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/checkin"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <CheckIn />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer/settlements"
              element={
                <ProtectedRoute role="ORGANIZER">
                  <Settlements />
                </ProtectedRoute>
              }
            />

          </Route>

          <Route element={<AdminLayout />}>
            <Route
              path="/admin"
              element={
                <ProtectedRoute role="ADMIN">
                  <AdminApprovals />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/settlements"
              element={
                <ProtectedRoute role="ADMIN">
                  <AdminSettlementDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/settings"
              element={
                <ProtectedRoute role="ADMIN">
                  <AdminPgSettings />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/users"
              element={
                <ProtectedRoute role="ADMIN">
                  <AdminUsers />
                </ProtectedRoute>
              }
            />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
