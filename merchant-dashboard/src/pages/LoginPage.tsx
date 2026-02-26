import { useState } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { Link, useNavigate } from 'react-router-dom'
import './AuthPages.css'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  
  const { login } = useAuth()
  const navigate = useNavigate()
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    
    try {
      await login(email, password)
      navigate('/dashboard')
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }
  
  return (
    <div className="auth-page">
      <div className="auth-page__shell">
        <aside className="auth-page__aside">
          <div>
            <div className="auth-page__brand">
              <span className="auth-page__brand-mark">PS</span>
              <span>Payment System</span>
            </div>
            <div className="auth-page__aside-copy">
              <h1>Merchant operations in one place</h1>
              <p>
                Track payments, manage refunds, monitor webhooks, and operate your payment stack from a single control
                surface.
              </p>
            </div>
          </div>

          <div className="auth-page__feature-list">
            <div className="auth-page__feature">
              <strong>Real-time statuses</strong>
              Payment and refund lifecycle tracking with operational actions.
            </div>
            <div className="auth-page__feature">
              <strong>Developer tools</strong>
              Webhooks, API keys, and analytics built into the merchant dashboard.
            </div>
          </div>
        </aside>

        <main className="auth-page__main">
          <div className="auth-card">
            <div className="auth-card__header">
              <h2>Welcome back</h2>
              <p>Log in to manage your merchant account and payment operations.</p>
            </div>

            {error && <div className="auth-card__error">{error}</div>}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="auth-form__grid">
                <div className="auth-form__field">
                  <label htmlFor="login-email">Email</label>
                  <input
                    id="login-email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoComplete="email"
                  />
                </div>

                <div className="auth-form__field">
                  <label htmlFor="login-password">Password</label>
                  <input
                    id="login-password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    autoComplete="current-password"
                  />
                </div>
              </div>

              <button type="submit" disabled={loading} className="auth-form__submit">
                {loading ? 'Logging in...' : 'Login'}
              </button>
            </form>

            <div className="auth-card__footer">
              Don't have an account? <Link to="/register">Register</Link>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
