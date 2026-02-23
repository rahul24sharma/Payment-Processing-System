import { useState } from 'react'
import CreateApiKeyForm from '@/components/apiKeys/CreateApiKeyForm'
import ApiKeysList from '@/components/apiKeys/ApiKeysList'
import ApiKeysGuide from '@/components/apiKeys/ApiKeysGuide'

export default function ApiKeysPage() {
  const [showCreateForm, setShowCreateForm] = useState(false)
  
  return (
    <div style={{ margin: '20px' }}>
      <div style={{ marginBottom: '30px' }}>
        <h1 style={{ marginBottom: '10px' }}>API Keys</h1>
        <p style={{ color: '#666', fontSize: '14px' }}>
          Manage your API keys for authentication. Keep your keys secure and never share them publicly.
        </p>
      </div>
      
      {/* Guide */}
      <ApiKeysGuide />
      
      {/* Action Button */}
      <div style={{ marginBottom: '20px' }}>
        <button
          onClick={() => setShowCreateForm(true)}
          style={{
            padding: '12px 24px',
            background: '#28a745',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontWeight: 'bold',
            fontSize: '14px',
          }}
        >
          + Generate New API Key
        </button>
      </div>
      
      {/* API Keys List */}
      <ApiKeysList />
      
      {/* Create Form Modal */}
      {showCreateForm && (
        <CreateApiKeyForm onClose={() => setShowCreateForm(false)} />
      )}
      
      {/* Usage Instructions */}
      <div style={{
        marginTop: '40px',
        border: '1px solid #ddd',
        borderRadius: '8px',
        padding: '20px',
      }}>
        <h3>Quick Start</h3>
        
        <div style={{ marginBottom: '20px' }}>
          <h4>1. Include your API key in requests:</h4>
          <pre style={{
            background: '#f5f5f5',
            padding: '15px',
            borderRadius: '4px',
            overflow: 'auto',
            fontSize: '12px',
          }}>
{`Authorization: Bearer sk_test_your_api_key_here`}
          </pre>
        </div>
        
        <div style={{ marginBottom: '20px' }}>
          <h4>2. Example: Create a payment</h4>
          <pre style={{
            background: '#f5f5f5',
            padding: '15px',
            borderRadius: '4px',
            overflow: 'auto',
            fontSize: '12px',
          }}>
{`curl -X POST https://api.yourpayment.com/v1/payments \\
  -H "Authorization: Bearer YOUR_API_KEY" \\
  -H "Idempotency-Key: $(uuidgen)" \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": 10000,
    "currency": "USD",
    "capture": true,
    "paymentMethod": {
      "type": "card",
      "cardToken": "tok_visa_4242"
    }
  }'`}
          </pre>
        </div>
        
        <div>
          <h4>3. Test Cards (Test Mode Only):</h4>
          <table style={{ width: '100%', fontSize: '12px' }}>
            <thead>
              <tr style={{ background: '#f5f5f5' }}>
                <th style={{ padding: '8px', textAlign: 'left' }}>Card Number</th>
                <th style={{ padding: '8px', textAlign: 'left' }}>Result</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td style={{ padding: '8px' }}><code>4242 4242 4242 4242</code></td>
                <td style={{ padding: '8px', color: '#28a745' }}>✓ Success</td>
              </tr>
              <tr>
                <td style={{ padding: '8px' }}><code>4000 0000 0000 0002</code></td>
                <td style={{ padding: '8px', color: '#dc3545' }}>✗ Card Declined</td>
              </tr>
              <tr>
                <td style={{ padding: '8px' }}><code>4000 0000 0000 9995</code></td>
                <td style={{ padding: '8px', color: '#dc3545' }}>✗ Insufficient Funds</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}