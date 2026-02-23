import { useState } from 'react'
import { useCreateApiKey } from '@/hooks/useApiKeys'

export default function CreateApiKeyForm({ onClose }: { onClose: () => void }) {
  const [name, setName] = useState('')
  const [isLive, setIsLive] = useState(false)
  const [generatedKey, setGeneratedKey] = useState<string | null>(null)
  
  const createApiKey = useCreateApiKey()
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      const result = await createApiKey.mutateAsync({ name, isLive })
      setGeneratedKey(result.key)
    } catch (error: any) {
      alert(`Error: ${error.response?.data?.error?.message || error.message}`)
    }
  }
  
  const handleCopy = () => {
    if (generatedKey) {
      navigator.clipboard.writeText(generatedKey)
      alert('API key copied to clipboard!')
    }
  }
  
  const handleDone = () => {
    setGeneratedKey(null)
    setName('')
    setIsLive(false)
    onClose()
  }
  
  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0,0,0,0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
    }}>
      <div style={{
        background: 'white',
        padding: '30px',
        borderRadius: '8px',
        maxWidth: '600px',
        width: '100%',
      }}>
        {!generatedKey ? (
          <>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h2 style={{ margin: 0 }}>Generate API Key</h2>
              <button
                onClick={onClose}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '24px',
                  cursor: 'pointer',
                }}
              >
                ×
              </button>
            </div>
            
            <form onSubmit={handleSubmit}>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
                  Key Name
                </label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g., Production Server, Development, Mobile App"
                  required
                  style={{
                    width: '100%',
                    padding: '10px',
                    border: '1px solid #ddd',
                    borderRadius: '4px',
                  }}
                />
                <small style={{ color: '#666', fontSize: '12px' }}>
                  Give this key a descriptive name to identify where it's used
                </small>
              </div>
              
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={isLive}
                    onChange={(e) => setIsLive(e.target.checked)}
                    style={{ marginRight: '10px' }}
                  />
                  <div>
                    <div style={{ fontWeight: 'bold' }}>
                      Live Mode {isLive && '(Real payments)'}
                    </div>
                    <small style={{ color: '#666', fontSize: '12px' }}>
                      {isLive 
                        ? '⚠️ This key will process real payments and charge real cards'
                        : '✓ This key is for testing only (no real charges)'}
                    </small>
                  </div>
                </label>
              </div>
              
              <div style={{
                background: isLive ? '#fff3cd' : '#d1ecf1',
                padding: '15px',
                borderRadius: '4px',
                marginBottom: '20px',
              }}>
                <strong>{isLive ? '⚠️ Warning:' : 'ℹ️ Info:'}</strong>
                {isLive 
                  ? ' Live mode keys can process real payments. Keep them secure!'
                  : ' Test mode keys use test cards and won\'t charge real money.'}
              </div>
              
              <div style={{ display: 'flex', gap: '10px' }}>
                <button
                  type="submit"
                  disabled={createApiKey.isPending}
                  style={{
                    flex: 1,
                    padding: '12px',
                    background: createApiKey.isPending ? '#ccc' : (isLive ? '#dc3545' : '#007bff'),
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: createApiKey.isPending ? 'not-allowed' : 'pointer',
                    fontWeight: 'bold',
                  }}
                >
                  {createApiKey.isPending ? 'Generating...' : 'Generate API Key'}
                </button>
                
                <button
                  type="button"
                  onClick={onClose}
                  style={{
                    padding: '12px 24px',
                    background: '#6c757d',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                  }}
                >
                  Cancel
                </button>
              </div>
            </form>
          </>
        ) : (
          <>
            <h2 style={{ color: '#28a745', marginBottom: '20px' }}>✅ API Key Generated!</h2>
            
            <div style={{
              background: '#fff3cd',
              padding: '15px',
              borderRadius: '4px',
              marginBottom: '20px',
              border: '2px solid #ffc107',
            }}>
              <strong>⚠️ Important:</strong> This is the only time you'll see this key. 
              Copy it now and store it securely!
            </div>
            
            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
                Your API Key:
              </label>
              <div style={{
                display: 'flex',
                gap: '10px',
                alignItems: 'center',
              }}>
                <code style={{
                  flex: 1,
                  background: '#f5f5f5',
                  padding: '12px',
                  borderRadius: '4px',
                  fontFamily: 'monospace',
                  fontSize: '14px',
                  wordBreak: 'break-all',
                  border: '2px solid #28a745',
                }}>
                  {generatedKey}
                </code>
                <button
                  onClick={handleCopy}
                  style={{
                    padding: '12px 20px',
                    background: '#007bff',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    whiteSpace: 'nowrap',
                  }}
                >
                  📋 Copy
                </button>
              </div>
            </div>
            
            <div style={{
              background: '#f5f5f5',
              padding: '15px',
              borderRadius: '4px',
              marginBottom: '20px',
            }}>
              <div style={{ fontWeight: 'bold', marginBottom: '10px' }}>Quick Start:</div>
              <pre style={{ fontSize: '12px', overflow: 'auto' }}>
{`curl -X POST http://localhost:8080/api/v1/payments \\
  -H "Authorization: Bearer ${generatedKey}" \\
  -H "Idempotency-Key: \$(uuidgen)" \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": 10000,
    "currency": "USD",
    "capture": true,
    "paymentMethod": {"type": "card", "cardToken": "tok_visa"}
  }'`}
              </pre>
            </div>
            
            <button
              onClick={handleDone}
              style={{
                width: '100%',
                padding: '12px',
                background: '#28a745',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontWeight: 'bold',
              }}
            >
              Done
            </button>
          </>
        )}
      </div>
    </div>
  )
}