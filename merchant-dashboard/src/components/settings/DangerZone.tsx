import { useState } from 'react'
import { useDeleteAccount } from '@/hooks/useSettings'
import { useAuth } from '@/contexts/AuthContext'
import { useToast } from '@/contexts/ToastContext'
import { useNavigate } from 'react-router-dom'
import './Settings.css'

export default function DangerZone() {
  const [confirmText, setConfirmText] = useState('')
  const [showConfirm, setShowConfirm] = useState(false)
  
  const deleteAccount = useDeleteAccount()
  const { logout } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  
  const handleDelete = async () => {
    if (confirmText !== 'DELETE MY ACCOUNT') {
      toast.error('Please type "DELETE MY ACCOUNT" to confirm')
      return
    }
    
    try {
      await deleteAccount.mutateAsync()
      
      toast.success('Your account has been deleted. You will be logged out.')
      
      logout()
      navigate('/login')
    } catch (error: any) {
      toast.error(error.message)
    }
  }
  
  return (
    <div className="settings-danger">
      <h3 className="settings-danger__title">⚠️ Danger Zone</h3>

      <div className="settings-danger__section">
        <h4>Delete Account</h4>
        <p>
          Once you delete your account, there is no going back. All your data including
          payments, customers, and API keys will be permanently deleted.
        </p>
      </div>

      {!showConfirm ? (
        <button
          onClick={() => setShowConfirm(true)}
          className="settings-btn settings-btn--danger"
          type="button"
        >
          Delete Account
        </button>
      ) : (
        <div className="settings-danger__confirm">
          <div className="settings-callout settings-callout--danger">
            <strong>This action cannot be undone!</strong>
            <br />
            Type <code className="settings-code">DELETE MY ACCOUNT</code> to confirm:
          </div>

          <input
            type="text"
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            placeholder="Type: DELETE MY ACCOUNT"
          />

          <div className="settings-form__actions">
            <button
              onClick={handleDelete}
              disabled={confirmText !== 'DELETE MY ACCOUNT' || deleteAccount.isPending}
              className="settings-btn settings-btn--danger"
              type="button"
            >
              {deleteAccount.isPending ? 'Deleting...' : 'Permanently Delete Account'}
            </button>

            <button
              onClick={() => {
                setShowConfirm(false)
                setConfirmText('')
              }}
              className="settings-btn settings-btn--neutral"
              type="button"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
