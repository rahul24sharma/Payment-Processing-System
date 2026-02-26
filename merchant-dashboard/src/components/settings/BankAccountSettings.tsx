import { useEffect, useState } from 'react'
import { useMerchantProfile, useUpdateBankAccount } from '@/hooks/useSettings'
import { useToast } from '@/contexts/ToastContext'
import './Settings.css'

export default function BankAccountSettings() {
  const { data: profile } = useMerchantProfile()
  const updateBankAccount = useUpdateBankAccount()
  const toast = useToast()
  const [accountHolderName, setAccountHolderName] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [routingNumber, setRoutingNumber] = useState('')
  const [accountType, setAccountType] = useState<'checking' | 'savings'>('checking')

  useEffect(() => {
    if (!profile?.bankAccount) return
    setAccountHolderName(profile.bankAccount.accountHolderName || '')
    // Do not hydrate sensitive bank numbers back into the form from profile responses.
    setAccountNumber('')
    setRoutingNumber('')
    setAccountType((profile.bankAccount.accountType as 'checking' | 'savings') || 'checking')
  }, [profile])
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      await updateBankAccount.mutateAsync({
        accountHolderName,
        accountNumber,
        routingNumber,
        accountType,
      })
      toast.success('Bank account updated! Payouts will be sent to this account.')
    } catch (error: any) {
      toast.error(error.response?.data?.error || error.message)
    }
  }
  
  return (
    <div className="settings-card">
      <div className="settings-card__header">
        <h3 className="settings-card__title">Bank Account (for Settlements)</h3>
        <p className="settings-card__subtitle">Configure the payout destination used for settlement transfers.</p>
      </div>

      <div className="settings-callout settings-callout--info">
        <strong>ℹ️ Info:</strong> Payouts will be sent to this bank account via ACH transfer (T+2 settlement).
      </div>

      {profile?.bankAccount?.accountNumber && (
        <div className="settings-callout settings-callout--info" role="status" aria-live="polite">
          <strong>Current account on file:</strong>{' '}
          {profile.bankAccount.accountType || 'bank'} ending in{' '}
          {profile.bankAccount.accountNumberLast4 || String(profile.bankAccount.accountNumber).slice(-4)}
          {profile.bankAccount.routingNumberLast4
            ? ` • Routing ending in ${profile.bankAccount.routingNumberLast4}`
            : ''}
        </div>
      )}

      <form onSubmit={handleSubmit} className="settings-form">
        <div className="settings-form__field">
          <label htmlFor="settings-account-holder">Account Holder Name *</label>
          <input
            id="settings-account-holder"
            type="text"
            value={accountHolderName}
            onChange={(e) => setAccountHolderName(e.target.value)}
            required
            placeholder="John Doe"
          />
        </div>

        <div className="settings-form__field">
          <label htmlFor="settings-account-type">Account Type *</label>
          <select
            id="settings-account-type"
            value={accountType}
            onChange={(e) => setAccountType(e.target.value as 'checking' | 'savings')}
          >
            <option value="checking">Checking</option>
            <option value="savings">Savings</option>
          </select>
        </div>

        <div className="settings-form__field">
          <label htmlFor="settings-routing-number">Routing Number *</label>
          <input
            id="settings-routing-number"
            type="text"
            value={routingNumber}
            onChange={(e) => setRoutingNumber(e.target.value)}
            required
            placeholder="110000000"
            maxLength={9}
            pattern="[0-9]{9}"
          />
          <small className="settings-form__hint">9-digit ABA routing number</small>
        </div>

        <div className="settings-form__field">
          <label htmlFor="settings-account-number">Account Number *</label>
          <input
            id="settings-account-number"
            type="text"
            value={accountNumber}
            onChange={(e) => setAccountNumber(e.target.value)}
            required
            placeholder="000123456789"
            maxLength={17}
          />
          <small className="settings-form__hint">
            Re-enter full bank details when updating the payout account
          </small>
        </div>

        <div className="settings-callout settings-callout--warning">
          <strong>🔒 Security:</strong> Bank account details are used only for settlement transfers.
          For production deployments, enable field-level encryption or tokenization at rest.
        </div>

        <button
          type="submit"
          className="settings-btn settings-btn--success"
          disabled={updateBankAccount.isPending}
        >
          {updateBankAccount.isPending ? 'Saving...' : 'Save Bank Account'}
        </button>
      </form>
    </div>
  )
}
