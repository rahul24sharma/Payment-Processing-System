import { useState } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { useToast } from '@/contexts/ToastContext'
import { Link, useNavigate } from 'react-router-dom'
import './AuthPages.css'

export default function RegisterPage() {
  const [businessName, setBusinessName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [createdApiKey, setCreatedApiKey] = useState<string | null>(null)
  const [copyingKey, setCopyingKey] = useState(false)
  
  const { register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    
    if (password !== confirmPassword) {
      setError('Passwords do not match')
      return
    }
    
    if (password.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }
    
    setLoading(true)
    
    try {
      const response = await register(businessName, email, password)
      if (response.apiKey) {
        setCreatedApiKey(response.apiKey)
        return
      }
      navigate('/dashboard')
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.message || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  const handleCopyApiKey = async () => {
    if (!createdApiKey) return
    try {
      setCopyingKey(true)
      await navigator.clipboard.writeText(createdApiKey)
      toast.success('API key copied to clipboard')
    } catch {
      toast.error('Unable to copy API key')
    } finally {
      setCopyingKey(false)
    }
  }

  const handleContinue = () => {
    setCreatedApiKey(null)
    navigate('/dashboard')
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
              <h1>Launch your merchant workspace</h1>
              <p>
                Create your account to start accepting payments, managing refunds, and configuring webhooks and API
                access for your backend.
              </p>
            </div>
          </div>

          <div className="auth-page__feature-list">
            <div className="auth-page__feature">
              <strong>Fast onboarding</strong>
              Create a merchant account and generate server-side credentials in one flow.
            </div>
            <div className="auth-page__feature">
              <strong>Operational tooling</strong>
              Built-in analytics, customers, webhooks, and API key management.
            </div>
          </div>
        </aside>

        <main className="auth-page__main">
          <div className="auth-card">
            <div className="auth-card__header">
              <h2>Create account</h2>
              <p>Set up your merchant workspace and start testing payment flows.</p>
            </div>

            {error && <div className="auth-card__error">{error}</div>}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="auth-form__grid">
                <div className="auth-form__field">
                  <label htmlFor="register-business-name">Business Name</label>
                  <input
                    id="register-business-name"
                    type="text"
                    value={businessName}
                    onChange={(e) => setBusinessName(e.target.value)}
                    required
                    autoComplete="organization"
                  />
                </div>

                <div className="auth-form__field">
                  <label htmlFor="register-email">Email</label>
                  <input
                    id="register-email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoComplete="email"
                  />
                </div>

                <div className="auth-form__field">
                  <label htmlFor="register-password">Password</label>
                  <input
                    id="register-password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    minLength={8}
                    autoComplete="new-password"
                  />
                  <small className="auth-form__hint">Minimum 8 characters</small>
                </div>

                <div className="auth-form__field">
                  <label htmlFor="register-confirm-password">Confirm Password</label>
                  <input
                    id="register-confirm-password"
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    autoComplete="new-password"
                  />
                </div>
              </div>

              <button type="submit" disabled={loading} className="auth-form__submit auth-form__submit--success">
                {loading ? 'Creating account...' : 'Register'}
              </button>
            </form>

            <div className="auth-card__footer">
              Already have an account? <Link to="/login">Login</Link>
            </div>
          </div>
        </main>
      </div>

      {createdApiKey && (
        <div className="auth-secret-modal" role="dialog" aria-modal="true" aria-labelledby="api-key-created-title">
          <div className="auth-secret-modal__backdrop" />
          <div className="auth-secret-modal__panel">
            <p className="auth-secret-modal__eyebrow">Registration Complete</p>
            <h3 id="api-key-created-title">Save your API key now</h3>
            <p className="auth-secret-modal__desc">
              This is the only time the full secret key will be shown. Store it securely and use it from your backend only.
            </p>

            <div className="auth-secret-modal__key-wrap">
              <code>{createdApiKey}</code>
            </div>

            <div className="auth-secret-modal__actions">
              <button type="button" className="auth-secret-modal__btn auth-secret-modal__btn--secondary" onClick={handleCopyApiKey} disabled={copyingKey}>
                {copyingKey ? 'Copying...' : 'Copy key'}
              </button>
              <button type="button" className="auth-secret-modal__btn auth-secret-modal__btn--primary" onClick={handleContinue}>
                I have saved it
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
