import { useMemo, useState } from 'react'
import { Link, useSearchParams, useNavigate } from 'react-router-dom'
import { authApi } from '@/api/auth'
import './AuthPages.css'

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const tokenFromQuery = useMemo(() => searchParams.get('token') || '', [searchParams])

  const [token, setToken] = useState(tokenFromQuery)
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccessMessage('')

    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match')
      return
    }

    setLoading(true)
    try {
      const response = await authApi.resetPassword({ token, newPassword })
      setSuccessMessage(response.message)
      setTimeout(() => navigate('/login'), 1200)
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.message || 'Unable to reset password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-page__shell auth-page__shell--single">
        <main className="auth-page__main auth-page__main--single">
          <div className="auth-card">
            <div className="auth-card__header">
              <h2>Reset password</h2>
              <p>Set a new password for your account.</p>
            </div>

            {error && <div className="auth-card__error">{error}</div>}
            {successMessage && <div className="auth-card__success">{successMessage}</div>}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="auth-form__grid">
                <div className="auth-form__field">
                  <label htmlFor="reset-token">Reset token</label>
                  <input
                    id="reset-token"
                    type="text"
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    required
                    autoComplete="off"
                  />
                </div>

                <div className="auth-form__field">
                  <label htmlFor="reset-new-password">New password</label>
                  <div className="auth-form__password-wrap">
                    <input
                      id="reset-new-password"
                      type={showNewPassword ? 'text' : 'password'}
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      required
                      minLength={8}
                      autoComplete="new-password"
                    />
                    <button
                      type="button"
                      className="auth-form__password-toggle"
                      onClick={() => setShowNewPassword((prev) => !prev)}
                      aria-label={showNewPassword ? 'Hide password' : 'Show password'}
                    >
                      {showNewPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>
                </div>

                <div className="auth-form__field">
                  <label htmlFor="reset-confirm-password">Confirm password</label>
                  <div className="auth-form__password-wrap">
                    <input
                      id="reset-confirm-password"
                      type={showConfirmPassword ? 'text' : 'password'}
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      required
                      minLength={8}
                      autoComplete="new-password"
                    />
                    <button
                      type="button"
                      className="auth-form__password-toggle"
                      onClick={() => setShowConfirmPassword((prev) => !prev)}
                      aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                    >
                      {showConfirmPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>
                </div>
              </div>

              <button type="submit" disabled={loading} className="auth-form__submit">
                {loading ? 'Resetting...' : 'Reset password'}
              </button>
            </form>

            <div className="auth-card__footer">
              Back to <Link to="/login">Login</Link>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
