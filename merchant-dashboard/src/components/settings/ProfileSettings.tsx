import { useState, useEffect } from 'react'
import { useMerchantProfile, useUpdateProfile } from '@/hooks/useSettings'
import { useToast } from '@/contexts/ToastContext'
import './Settings.css'

export default function ProfileSettings() {
  const { data: profile } = useMerchantProfile()
  const updateProfile = useUpdateProfile()
  const toast = useToast()
  
  const [businessName, setBusinessName] = useState('')
  const [phone, setPhone] = useState('')
  const [website, setWebsite] = useState('')
  
  useEffect(() => {
    if (profile) {
      setBusinessName(profile.businessName || '')
      setPhone(profile.phone || '')
      setWebsite(profile.website || '')
    }
  }, [profile])
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      await updateProfile.mutateAsync({
        businessName,
        phone,
        website,
      })
      
      toast.success('Profile updated successfully!')
    } catch (error: any) {
      toast.error(error.response?.data?.error?.message || error.message)
    }
  }
  
  return (
    <div className="settings-card">
      <div className="settings-card__header">
        <h3 className="settings-card__title">Business Profile</h3>
        <p className="settings-card__subtitle">Update public business identity and contact details.</p>
      </div>

      <form onSubmit={handleSubmit} className="settings-form">
        <div className="settings-form__field">
          <label htmlFor="settings-business-name">Business Name *</label>
          <input
            id="settings-business-name"
            type="text"
            value={businessName}
            onChange={(e) => setBusinessName(e.target.value)}
            required
          />
        </div>

        <div className="settings-form__field">
          <label htmlFor="settings-email">Email (Read-only)</label>
          <input
            id="settings-email"
            type="email"
            value={profile?.email || ''}
            disabled
          />
          <small className="settings-form__hint">
            Email cannot be changed. Contact support if you need to update it.
          </small>
        </div>

        <div className="settings-form__field">
          <label htmlFor="settings-phone">Phone</label>
          <input
            id="settings-phone"
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="+1 (555) 123-4567"
          />
        </div>

        <div className="settings-form__field">
          <label htmlFor="settings-website">Website</label>
          <input
            id="settings-website"
            type="url"
            value={website}
            onChange={(e) => setWebsite(e.target.value)}
            placeholder="https://your-business.com"
          />
        </div>

        <div className="settings-form__actions">
          <button
            type="submit"
            disabled={updateProfile.isPending}
            className="settings-btn settings-btn--primary"
          >
            {updateProfile.isPending ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </form>
    </div>
  )
}
