import { useState } from 'react'
import CreateApiKeyForm from '@/components/apiKeys/CreateApiKeyForm'
import ApiKeysList from '@/components/apiKeys/ApiKeysList'
import ApiKeysGuide from '@/components/apiKeys/ApiKeysGuide'
import './ApiKeysPage.css'

export default function ApiKeysPage() {
  const [showCreateForm, setShowCreateForm] = useState(false)
  
  return (
    <div className="api-keys-page">
      <section className="api-keys-page__hero">
        <div>
          <p className="api-keys-page__eyebrow">Developer Access</p>
          <h1>API Keys</h1>
          <p className="api-keys-page__subtitle">
            Generate, revoke, and audit secret keys used by your backend services and server-side integrations.
          </p>
        </div>
        <div className="api-keys-page__hero-card">
          <div className="api-keys-page__hero-card-label">Security Rule</div>
          <div className="api-keys-page__hero-card-value">Server-side only</div>
          <div className="api-keys-page__hero-card-note">Never expose secret keys in client code</div>
        </div>
      </section>

      <ApiKeysGuide />

      <div className="api-keys-page__actions">
        <button
          onClick={() => setShowCreateForm(true)}
          className="api-keys-page__primary-btn"
          type="button"
        >
          + Generate New API Key
        </button>
      </div>

      <ApiKeysList />

      {showCreateForm && (
        <CreateApiKeyForm onClose={() => setShowCreateForm(false)} />
      )}

      <section className="api-keys-page__quickstart">
        <div className="api-keys-page__quickstart-header">
          <p className="api-keys-page__eyebrow">Quick Start</p>
          <h3>Authenticate requests and test safely</h3>
          <p>
            Use a secret key in your backend service. Pair it with an idempotency key for payment creation and never
            ship secret keys to browsers or mobile apps.
          </p>
        </div>

        <div className="api-keys-page__quickstart-grid">
          <div className="api-keys-page__snippet-card">
            <h4>1. Authorization Header</h4>
            <pre>{`Authorization: Bearer sk_test_your_api_key_here`}</pre>
          </div>

          <div className="api-keys-page__snippet-card api-keys-page__snippet-card--wide">
            <h4>2. Example: Create a payment (server-side)</h4>
            <pre>{`curl -X POST http://localhost:8080/api/v1/payments \\
  -H "Authorization: Bearer YOUR_API_KEY" \\
  -H "Idempotency-Key: $(uuidgen)" \\
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
  }'`}</pre>
          </div>

          <div className="api-keys-page__snippet-card">
            <h4>3. Test Card Numbers</h4>
            <table className="api-keys-page__cards-table">
              <thead>
                <tr>
                  <th>Card Number</th>
                  <th>Result</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td><code>4242 4242 4242 4242</code></td>
                  <td className="api-keys-page__result--success">Success</td>
                </tr>
                <tr>
                  <td><code>4000 0000 0000 0002</code></td>
                  <td className="api-keys-page__result--danger">Card Declined</td>
                </tr>
                <tr>
                  <td><code>4000 0000 0000 9995</code></td>
                  <td className="api-keys-page__result--danger">Insufficient Funds</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  )
}
