import { useState } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '@/api/auth'
import './AuthPages.css'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [resetToken, setResetToken] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccessMessage('')
    setResetToken(null)
    setLoading(true)

    try {
      const response = await authApi.forgotPassword({ email })
      setSuccessMessage(response.message)
      if (response.resetToken) {
        setResetToken(response.resetToken)
      }
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.message || 'Unable to request password reset')
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
              <span className="auth-page__brand-mark">PP</span>
              <span>PulsePay</span>
            </div>
            <div className="auth-page__aside-copy">
              <h1>Recover account access securely</h1>
              <p>
                Submit your account email to generate a reset flow. We keep responses generic to protect
                against account enumeration.
              </p>
            </div>
          </div>

          <div className="auth-page__feature-list">
            <div className="auth-page__feature">
              <strong>Security first</strong>
              Reset tokens are time-bound and single-use.
            </div>
            <div className="auth-page__feature">
              <strong>Fast recovery</strong>
              Complete reset and return to your dashboard in minutes.
            </div>
          </div>
        </aside>

        <main className="auth-page__main">
          <div className="auth-card">
            <div className="auth-card__header">
              <h2>Forgot password</h2>
              <p>Enter your account email to receive password reset instructions.</p>
            </div>

            {error && <div className="auth-card__error">{error}</div>}
            {successMessage && <div className="auth-card__success">{successMessage}</div>}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="auth-form__field">
                <label htmlFor="forgot-email">Email</label>
                <input
                  id="forgot-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                />
              </div>

              <button type="submit" disabled={loading} className="auth-form__submit">
                {loading ? 'Submitting...' : 'Send reset instructions'}
              </button>
            </form>

            {resetToken && (
              <div className="auth-card__token-box">
                <p>Local dev reset token:</p>
                <code>{resetToken}</code>
                <Link to={`/reset-password?token=${encodeURIComponent(resetToken)}`} className="auth-card__token-link">
                  Continue to reset password
                </Link>
              </div>
            )}

            <div className="auth-card__footer">
              Remembered your password? <Link to="/login">Back to login</Link>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
