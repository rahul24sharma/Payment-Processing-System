import { useState, useEffect } from 'react'
import { useMerchantProfile, useUpdateNotificationPreferences } from '@/hooks/useSettings'
import { useToast } from '@/contexts/ToastContext'
import './Settings.css'

export default function NotificationSettings() {
  const { data: profile } = useMerchantProfile()
  const updateNotificationPreferences = useUpdateNotificationPreferences()
  const toast = useToast()
  
  const [emailOnPayment, setEmailOnPayment] = useState(true)
  const [emailOnRefund, setEmailOnRefund] = useState(true)
  const [emailOnPayout, setEmailOnPayout] = useState(true)
  const [emailOnFraud, setEmailOnFraud] = useState(true)
  
  useEffect(() => {
    if (profile?.notifications) {
      setEmailOnPayment(profile.notifications.emailOnPayment ?? true)
      setEmailOnRefund(profile.notifications.emailOnRefund ?? true)
      setEmailOnPayout(profile.notifications.emailOnPayout ?? true)
      setEmailOnFraud(profile.notifications.emailOnFraud ?? true)
    }
  }, [profile])
  
  const handleSave = async () => {
    try {
      await updateNotificationPreferences.mutateAsync({
        emailOnPayment,
        emailOnRefund,
        emailOnPayout,
        emailOnFraud,
      })
      toast.success('Notification preferences saved!')
    } catch (error: any) {
      toast.error(error.response?.data?.error || error.message)
    }
  }
  
  return (
    <div className="settings-card">
      <div className="settings-card__header">
        <h3 className="settings-card__title">Email Notifications</h3>
        <p className="settings-card__subtitle">Choose which operational events should trigger email notifications.</p>
      </div>

      <div className="settings-toggle-list">
        <label className="settings-toggle">
          <input type="checkbox" checked={emailOnPayment} onChange={(e) => setEmailOnPayment(e.target.checked)} />
          <div>
            <div className="settings-toggle__title">Payment Captured</div>
            <div className="settings-toggle__desc">Receive emails when payments are successfully captured</div>
          </div>
        </label>

        <label className="settings-toggle">
          <input type="checkbox" checked={emailOnRefund} onChange={(e) => setEmailOnRefund(e.target.checked)} />
          <div>
            <div className="settings-toggle__title">Refunds</div>
            <div className="settings-toggle__desc">Receive emails when refunds are processed</div>
          </div>
        </label>

        <label className="settings-toggle">
          <input type="checkbox" checked={emailOnPayout} onChange={(e) => setEmailOnPayout(e.target.checked)} />
          <div>
            <div className="settings-toggle__title">Payouts</div>
            <div className="settings-toggle__desc">Receive emails when payouts are transferred to your bank</div>
          </div>
        </label>

        <label className="settings-toggle settings-toggle--warning">
          <input type="checkbox" checked={emailOnFraud} onChange={(e) => setEmailOnFraud(e.target.checked)} />
          <div>
            <div className="settings-toggle__title">Fraud Alerts</div>
            <div className="settings-toggle__desc">Recommended: high-risk payment alerts and anomaly notifications</div>
          </div>
        </label>
      </div>

      <div className="settings-form__actions" style={{ marginTop: '14px' }}>
        <button
          onClick={handleSave}
          className="settings-btn settings-btn--primary"
          type="button"
          disabled={updateNotificationPreferences.isPending}
        >
        {updateNotificationPreferences.isPending ? 'Saving...' : 'Save Preferences'}
        </button>
      </div>
    </div>
  )
}
