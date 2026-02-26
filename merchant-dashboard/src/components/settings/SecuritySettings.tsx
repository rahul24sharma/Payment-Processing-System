import { useState } from 'react'
import { useChangePassword } from '@/hooks/useSettings'
import { useToast } from '@/contexts/ToastContext'
import './Settings.css'

export default function SecuritySettings() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  
  const changePassword = useChangePassword()
  const toast = useToast()
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (newPassword !== confirmPassword) {
      toast.error('New passwords do not match')
      return
    }
    
    if (newPassword.length < 8) {
      toast.error('Password must be at least 8 characters')
      return
    }
    
    try {
      await changePassword.mutateAsync({
        currentPassword,
        newPassword,
        confirmPassword,
      })
      
      toast.success('Password changed successfully!')
      
      // Reset form
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (error: any) {
      toast.error(error.response?.data?.error || error.message)
    }
  }
  
  return (
    <div className="settings-card">
      <div className="settings-card__header">
        <h3 className="settings-card__title">Change Password</h3>
        <p className="settings-card__subtitle">Update your account password and maintain strong credentials.</p>
      </div>

      <form onSubmit={handleSubmit} className="settings-form">
        <div className="settings-form__field">
          <label htmlFor="settings-current-password">Current Password *</label>
          <input
            id="settings-current-password"
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
          />
        </div>

        <div className="settings-form__field">
          <label htmlFor="settings-new-password">New Password *</label>
          <input
            id="settings-new-password"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            minLength={8}
          />
          <small className="settings-form__hint">Minimum 8 characters</small>
        </div>

        <div className="settings-form__field">
          <label htmlFor="settings-confirm-password">Confirm New Password *</label>
          <input
            id="settings-confirm-password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />
        </div>

        <button
          type="submit"
          disabled={changePassword.isPending}
          className="settings-btn settings-btn--danger"
        >
          {changePassword.isPending ? 'Changing...' : 'Change Password'}
        </button>
      </form>
    </div>
  )
}
