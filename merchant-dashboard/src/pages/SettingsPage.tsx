import { useState } from 'react'
import AccountInfo from '@/components/settings/AccountInfo'
import ProfileSettings from '@/components/settings/ProfileSettings'
import SecuritySettings from '@/components/settings/SecuritySettings'
import NotificationSettings from '@/components/settings/NotificationSettings'
import BankAccountSettings from '@/components/settings/BankAccountSettings'
import DangerZone from '@/components/settings/DangerZone'
import './SettingsPage.css'

type SettingsTab = 'profile' | 'security' | 'notifications' | 'bank' | 'danger'

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<SettingsTab>('profile')
  
  const tabs = [
    { id: 'profile' as SettingsTab, label: 'Profile', icon: '👤' },
    { id: 'security' as SettingsTab, label: 'Security', icon: '🔒' },
    { id: 'notifications' as SettingsTab, label: 'Notifications', icon: '🔔' },
    { id: 'bank' as SettingsTab, label: 'Bank Account', icon: '🏦' },
    { id: 'danger' as SettingsTab, label: 'Danger Zone', icon: '⚠️' },
  ]
  
  return (
    <div className="settings-page">
      <section className="settings-page__hero">
        <div>
          <p className="settings-page__eyebrow">Account Administration</p>
          <h1>Settings</h1>
          <p className="settings-page__subtitle">
            Manage merchant profile data, security settings, notifications, payout account details, and account lifecycle
            controls.
          </p>
        </div>
        <div className="settings-page__hero-card">
          <div className="settings-page__hero-card-label">Workspace Focus</div>
          <div className="settings-page__hero-card-value">
            {tabs.find((tab) => tab.id === activeTab)?.label}
          </div>
          <div className="settings-page__hero-card-note">Operational preferences and account security</div>
        </div>
      </section>

      <AccountInfo />

      <div className="settings-page__tabs">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`settings-page__tab ${activeTab === tab.id ? 'settings-page__tab--active' : ''}`}
            type="button"
          >
            <span>{tab.icon}</span>
            {tab.label}
          </button>
        ))}
      </div>

      <div className="settings-page__content">
        {activeTab === 'profile' && <ProfileSettings />}
        {activeTab === 'security' && <SecuritySettings />}
        {activeTab === 'notifications' && <NotificationSettings />}
        {activeTab === 'bank' && <BankAccountSettings />}
        {activeTab === 'danger' && <DangerZone />}
      </div>
    </div>
  )
}
