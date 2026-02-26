import './ApiKeys.css'

export default function ApiKeysGuide() {
  return (
    <section className="api-keys-guide">
      <div className="api-keys-guide__header">
        <h3>API Keys Guide</h3>
        <p>Use test keys for development and live keys only in secured backend environments.</p>
      </div>

      <div className="api-keys-guide__grid">
        <div className="api-keys-guide__mode api-keys-guide__mode--test">
          <h4>Test Mode Keys (`sk_test_...`)</h4>
          <ul>
            <li>Use for development and testing</li>
            <li>No real charges are made</li>
            <li>Use test card numbers (4242 4242 4242 4242)</li>
          </ul>
        </div>

        <div className="api-keys-guide__mode api-keys-guide__mode--live">
          <h4>Live Mode Keys (`sk_live_...`)</h4>
          <ul>
            <li>Process real payments</li>
            <li>Charge real credit cards</li>
            <li>Keep these secret and secure</li>
            <li>Never commit to version control</li>
          </ul>
        </div>
      </div>

      <details className="api-keys-guide__details">
        <summary>Security Best Practices</summary>
        <ul>
          <li>Never expose API keys in client-side code</li>
          <li>Use environment variables to store keys</li>
          <li>Rotate keys periodically</li>
          <li>Revoke keys immediately if compromised</li>
          <li>Use different keys for different environments</li>
          <li>Monitor API key usage regularly</li>
        </ul>
      </details>
    </section>
  )
}
