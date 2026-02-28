import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Routes, Route, Link, NavLink, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from '@/contexts/AuthContext'
import { ToastProvider } from '@/contexts/ToastContext'
import ProtectedRoute from '@/components/ProtectedRoute'
import { Suspense, lazy, useEffect, useRef, useState } from 'react'
import './App.css'

const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const ForgotPasswordPage = lazy(() => import('./pages/ForgotPasswordPage'))
const ResetPasswordPage = lazy(() => import('./pages/ResetPasswordPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const CreatePaymentPage = lazy(() => import('./pages/CreatePaymentPage'))
const PaymentsPage = lazy(() => import('./pages/PaymentsPage'))
const PaymentDetailPage = lazy(() => import('./pages/PaymentDetailPage'))
const RefundsPage = lazy(() => import('./pages/RefundsPage'))
const CustomersPage = lazy(() => import('./pages/CustomersPage'))
const TicketsPage = lazy(() => import('./pages/TicketsPage'))
const AnalyticsPage = lazy(() => import('./pages/AnalyticsPage'))
const WebhooksPage = lazy(() => import('./pages/WebhooksPage'))
const ApiKeysPage = lazy(() => import('./pages/ApiKeysPage'))
const SettingsPage = lazy(() => import('./pages/SettingsPage'))
const MLMonitoringPage = lazy(() => import('./pages/MLMonitoringPage'))

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
})

function AppContent() {
  const { isAuthenticated, email, role, logout } = useAuth()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [profileMenuOpen, setProfileMenuOpen] = useState(false)
  const profileMenuRef = useRef<HTMLDivElement | null>(null)
  const mlMonitoringEnabled = import.meta.env.VITE_ENABLE_ML_MONITORING === 'true'
  const roleUpper = role?.toUpperCase()
  const canManageDeveloperResources = roleUpper === 'ADMIN' || roleUpper === 'DEVELOPER'
  const canManageSettings = roleUpper === 'ADMIN'
  const canAccessTickets = roleUpper === 'ADMIN' || roleUpper === 'SUPPORT' || roleUpper === 'DEVELOPER'

  const topNavItems = [
    { to: '/dashboard', label: 'Dashboard' },
    { to: '/create-payment', label: 'Create Payment' },
    { to: '/payments', label: 'Payments' },
    { to: '/refunds', label: 'Refunds' },
  ]

  const sidebarNavItems = [
    { to: '/customers', label: 'Customers' },
    ...(canAccessTickets ? [{ to: '/tickets', label: 'Tickets' }] : []),
    { to: '/analytics', label: 'Analytics' },
    ...(canManageDeveloperResources ? [{ to: '/webhooks', label: 'Webhooks' }] : []),
    ...(canManageDeveloperResources ? [{ to: '/api-keys', label: 'API Keys' }] : []),
    ...(mlMonitoringEnabled ? [{ to: '/ml-monitoring', label: 'ML Monitoring' }] : []),
  ]

  useEffect(() => {
    if (!profileMenuOpen) return

    const onPointerDown = (event: MouseEvent) => {
      if (!profileMenuRef.current) return
      if (!profileMenuRef.current.contains(event.target as Node)) {
        setProfileMenuOpen(false)
      }
    }

    const onEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setProfileMenuOpen(false)
      }
    }

    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onEscape)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onEscape)
    }
  }, [profileMenuOpen])

  const userInitial = (email?.trim()?.[0] || 'U').toUpperCase()
  
  return (
    <div className="app-shell">
      {isAuthenticated && (
        <>
          <header className="topbar">
            <div className="topbar__left">
              <button
                type="button"
                className="topbar__menu-btn"
                onClick={() => setSidebarOpen((prev) => !prev)}
                aria-label="Toggle sidebar"
              >
                ☰
              </button>
              <Link to="/dashboard" className="brand">
                <span className="brand__mark">PP</span>
                <span>PulsePay</span>
              </Link>
              <nav className="topbar__nav" aria-label="Primary">
                {topNavItems.map((item) => (
                  <NavLink
                    key={`top-${item.to}`}
                    to={item.to}
                    className={({ isActive }) =>
                      `topbar__nav-link${isActive ? ' topbar__nav-link--active' : ''}`
                    }
                  >
                    {item.label}
                  </NavLink>
                ))}
              </nav>
            </div>

            <div className="topbar__right">
              <span className="topbar__email">{email}</span>
              <div className="profile-menu" ref={profileMenuRef}>
                <button
                  type="button"
                  className="profile-menu__trigger"
                  aria-haspopup="menu"
                  aria-expanded={profileMenuOpen}
                  onClick={() => setProfileMenuOpen((prev) => !prev)}
                >
                  <span className="profile-menu__avatar">{userInitial}</span>
                  <span className="profile-menu__chevron" aria-hidden="true">▾</span>
                </button>

                {profileMenuOpen && (
                  <div className="profile-menu__dropdown" role="menu" aria-label="Profile menu">
                    <div className="profile-menu__header">
                      <div className="profile-menu__avatar profile-menu__avatar--large">{userInitial}</div>
                      <div className="profile-menu__identity">
                        <div className="profile-menu__title">Account</div>
                        <div className="profile-menu__subtitle">{email}</div>
                      </div>
                    </div>

                    <div className="profile-menu__items">
                      {canManageSettings && (
                        <Link
                          to="/settings"
                          className="profile-menu__item"
                          role="menuitem"
                          onClick={() => setProfileMenuOpen(false)}
                        >
                          Settings
                        </Link>
                      )}
                      {canManageDeveloperResources && (
                        <Link
                          to="/api-keys"
                          className="profile-menu__item"
                          role="menuitem"
                          onClick={() => setProfileMenuOpen(false)}
                        >
                          API Keys
                        </Link>
                      )}
                      <button
                        type="button"
                        className="profile-menu__item profile-menu__item--danger"
                        role="menuitem"
                        onClick={() => {
                          setProfileMenuOpen(false)
                          logout()
                        }}
                      >
                        Logout
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </header>

          <div className="shell-body">
            <aside className={`sidebar ${sidebarOpen ? 'sidebar--open' : ''}`}>
              <div className="sidebar__mobile-main">
                <div className="sidebar__section-title">Main</div>
                <nav className="sidebar__nav">
                  {topNavItems.map((item) => (
                    <NavLink
                      key={`mobile-${item.to}`}
                      to={item.to}
                      className={({ isActive }) =>
                        `sidebar__link${isActive ? ' sidebar__link--active' : ''}`
                      }
                      onClick={() => setSidebarOpen(false)}
                    >
                      {item.label}
                    </NavLink>
                  ))}
                </nav>
              </div>

              <div className="sidebar__section-title">Workspace</div>
              <nav className="sidebar__nav">
                {sidebarNavItems.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) =>
                      `sidebar__link${isActive ? ' sidebar__link--active' : ''}`
                    }
                    onClick={() => setSidebarOpen(false)}
                  >
                    {item.label}
                  </NavLink>
                ))}
              </nav>
            </aside>

            {sidebarOpen && (
              <button
                type="button"
                className="sidebar-backdrop"
                aria-label="Close sidebar"
                onClick={() => setSidebarOpen(false)}
              />
            )}
          </div>
        </>
      )}
      
      <main className={`app-content ${isAuthenticated ? 'app-content--with-sidebar' : ''}`}>
        <div className="app-content__inner">
        <Suspense fallback={<RouteLoadingFallback />}>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          
          {/* Protected Routes */}
          <Route
            path="/"
            element={
              isAuthenticated ? <Navigate to="/dashboard" /> : <Navigate to="/login" />
            }
          />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/create-payment"
            element={
              <ProtectedRoute>
                <CreatePaymentPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/payments"
            element={
              <ProtectedRoute>
                <PaymentsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/payments/:id"
            element={
              <ProtectedRoute>
                <PaymentDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/webhooks"
            element={
              <ProtectedRoute requiredRoles={['ADMIN', 'DEVELOPER']}>
                <WebhooksPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/api-keys"
            element={
              <ProtectedRoute requiredRoles={['ADMIN', 'DEVELOPER']}>
                <ApiKeysPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tickets"
            element={
              <ProtectedRoute requiredRoles={['ADMIN', 'SUPPORT', 'DEVELOPER']}>
                <TicketsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/analytics"
            element={
              <ProtectedRoute>
                <AnalyticsPage />
              </ProtectedRoute>
            }
          />
          {mlMonitoringEnabled && (
            <Route
              path="/ml-monitoring"
              element={
                <ProtectedRoute>
                  <MLMonitoringPage />
                </ProtectedRoute>
              }
            />
          )}
          <Route
            path="/settings"
            element={
              <ProtectedRoute requiredRoles={['ADMIN']}>
                <SettingsPage />
              </ProtectedRoute>
            }
          />
            <Route
              path="/customers"
              element={
                <ProtectedRoute>
                  <CustomersPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/refunds"
              element={
                <ProtectedRoute>
                  <RefundsPage />
                </ProtectedRoute>
              }
            />
        </Routes>
        </Suspense>
        </div>
      </main>
    </div>
  )
}

function RouteLoadingFallback() {
  return (
    <div
      style={{
        padding: '18px',
        borderRadius: '16px',
        border: '1px solid rgba(15, 23, 42, 0.08)',
        background: 'white',
        color: '#475569',
      }}
    >
      Loading page...
    </div>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ToastProvider>
          <AuthProvider>
            <AppContent />
          </AuthProvider>
        </ToastProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
