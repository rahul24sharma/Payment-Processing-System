import { useMerchantProfile } from '@/hooks/useSettings'
import { formatDate } from '@/utils/formatters'
import './Settings.css'

export default function AccountInfo() {
  const { data: profile, isLoading } = useMerchantProfile()
  
  if (isLoading) {
    return <div className="settings-card">Loading account info...</div>
  }
  
  if (!profile) {
    return null
  }
  
  return (
    <div className="settings-card">
      <div className="settings-card__header">
        <h3 className="settings-card__title">Account Information</h3>
        <p className="settings-card__subtitle">Merchant profile summary and risk status snapshot</p>
      </div>

      <table className="settings-grid-table">
        <tbody>
          <tr>
            <td>Account ID:</td>
            <td><code className="settings-code">{profile.id}</code></td>
          </tr>
          <tr>
            <td>Business Name:</td>
            <td>{profile.businessName}</td>
          </tr>
          <tr>
            <td>Email:</td>
            <td>{profile.email}</td>
          </tr>
          <tr>
            <td>Status:</td>
            <td>
              <span className={`settings-pill ${profile.status === 'ACTIVE' ? 'settings-pill--success' : 'settings-pill--danger'}`}>
                {profile.status}
              </span>
            </td>
          </tr>
          <tr>
            <td>Risk Profile:</td>
            <td>
              <span className={`settings-pill ${getRiskProfileClass(profile.riskProfile)}`}>
                {profile.riskProfile || 'UNKNOWN'}
              </span>
            </td>
          </tr>
          <tr>
            <td>Account Created:</td>
            <td>{profile.createdAt ? formatDate(profile.createdAt) : 'N/A'}</td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}

function getRiskProfileClass(riskProfile?: string): string {
  switch (riskProfile?.toUpperCase()) {
    case 'LOW':
      return 'settings-pill--success'
    case 'MEDIUM':
      return 'settings-pill--warning'
    case 'HIGH':
      return 'settings-pill--danger'
    default:
      return 'settings-pill--neutral'
  }
}
