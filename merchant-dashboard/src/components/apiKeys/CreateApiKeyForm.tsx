import { useState } from 'react'
import { useCreateApiKey } from '@/hooks/useApiKeys'
import { useToast } from '@/contexts/ToastContext'
import './ApiKeys.css'

export default function CreateApiKeyForm({ onClose }: { onClose: () => void }) {
  const [name, setName] = useState('')
  const [isLive, setIsLive] = useState(false)
  const [generatedKey, setGeneratedKey] = useState<string | null>(null)
  const toast = useToast()
  
  const createApiKey = useCreateApiKey()
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      const result = await createApiKey.mutateAsync({ name, isLive })
      setGeneratedKey(result.key)
    } catch (error: any) {
      toast.error(error.response?.data?.error?.message || error.message)
    }
  }
  
  const handleCopy = () => {
    if (generatedKey) {
      navigator.clipboard.writeText(generatedKey)
      toast.success('API key copied to clipboard!')
    }
  }
  
  const handleDone = () => {
    setGeneratedKey(null)
    setName('')
    setIsLive(false)
    onClose()
  }
  
  return (
    <div className="api-key-modal" role="dialog" aria-modal="true" aria-label="Generate API key">
      <button className="api-key-modal__backdrop" onClick={onClose} aria-label="Close modal" type="button" />
      <div className="api-key-modal__panel">
        {!generatedKey ? (
          <>
            <div className="api-key-modal__header">
              <h2>Generate API Key</h2>
              <button onClick={onClose} className="api-key-modal__close" type="button" aria-label="Close">
                ×
              </button>
            </div>

            <form onSubmit={handleSubmit} className="api-key-form">
              <div className="api-key-form__field">
                <label htmlFor="api-key-name">Key Name</label>
                <input
                  id="api-key-name"
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g., Production Server, Development, Mobile App"
                  required
                />
                <small className="api-key-form__hint">
                  Give this key a descriptive name to identify where it's used
                </small>
              </div>

              <label className="api-key-form__toggle">
                  <input
                    type="checkbox"
                    checked={isLive}
                    onChange={(e) => setIsLive(e.target.checked)}
                  />
                  <div>
                    <div className="api-key-form__toggle-title">
                      Live Mode {isLive && '(Real payments)'}
                    </div>
                    <div className="api-key-form__toggle-subtitle">
                      {isLive 
                        ? 'This key will process real payments and charge real cards'
                        : 'This key is for testing only (no real charges)'}
                    </div>
                  </div>
              </label>

              <div className={`api-key-form__callout ${isLive ? 'api-key-form__callout--live' : 'api-key-form__callout--test'}`}>
                <strong>{isLive ? '⚠️ Warning:' : 'ℹ️ Info:'}</strong>
                {isLive 
                  ? ' Live mode keys can process real payments. Keep them secure!'
                  : ' Test mode keys use test cards and won\'t charge real money.'}
              </div>

              <div className="api-key-form__actions">
                <button
                  type="submit"
                  disabled={createApiKey.isPending}
                  className={`api-key-form__submit ${isLive ? 'api-key-form__submit--live' : ''}`}
                >
                  {createApiKey.isPending ? 'Generating...' : 'Generate API Key'}
                </button>

                <button
                  type="button"
                  onClick={onClose}
                  className="api-key-form__cancel"
                >
                  Cancel
                </button>
              </div>
            </form>
          </>
        ) : (
          <div className="api-key-form__result">
            <h2 className="api-key-form__result-title">API Key Generated</h2>

            <div className="api-key-form__warning">
              <strong>⚠️ Important:</strong> This is the only time you'll see this key. 
              Copy it now and store it securely!
            </div>

            <div className="api-key-form__field">
              <label>Your API Key</label>
              <div className="api-key-form__secret-row">
                <code className="api-key-form__secret-code">{generatedKey}</code>
                <button
                  onClick={handleCopy}
                  className="api-key-form__copy"
                  type="button"
                >
                  Copy
                </button>
              </div>
            </div>

            <div className="api-key-form__snippet">
              <div className="api-key-form__snippet-title">Quick Start</div>
              <pre>
{`curl -X POST http://localhost:8080/api/v1/payments \\
  -H "Authorization: Bearer ${generatedKey}" \\
  -H "Idempotency-Key: \$(uuidgen)" \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": 10000,
    "currency": "USD",
    "customer": {
      "email": "buyer@example.com",
      "name": "Test Buyer",
      "address": {
        "line1": "1 Demo Street",
        "city": "Mumbai",
        "state": "MH",
        "postalCode": "400001",
        "country": "IN"
      }
    },
    "capture": true
  }'`}
              </pre>
            </div>

            <button
              onClick={handleDone}
              className="api-key-form__done"
              type="button"
            >
              Done
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
