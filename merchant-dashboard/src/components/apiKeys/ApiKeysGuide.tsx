export default function ApiKeysGuide() {
    return (
      <div style={{
        border: '1px solid #d1ecf1',
        background: '#d1ecf1',
        padding: '20px',
        borderRadius: '8px',
        marginBottom: '30px',
      }}>
        <h3 style={{ marginTop: 0, color: '#0c5460' }}>API Keys Guide</h3>
        
        <div style={{ marginBottom: '15px', color: '#0c5460' }}>
          <strong>Test Mode Keys (sk_test_...)</strong>
          <ul style={{ marginTop: '5px', marginBottom: '10px' }}>
            <li>Use for development and testing</li>
            <li>No real charges are made</li>
            <li>Use test card numbers (4242 4242 4242 4242)</li>
          </ul>
        </div>
        
        <div style={{ marginBottom: '15px', color: '#0c5460' }}>
          <strong>Live Mode Keys (sk_live_...)</strong>
          <ul style={{ marginTop: '5px', marginBottom: '10px' }}>
            <li>Process real payments</li>
            <li>Charge real credit cards</li>
            <li>Keep these secret and secure</li>
            <li>Never commit to version control</li>
          </ul>
        </div>
        
        <details style={{ marginTop: '15px' }}>
          <summary style={{ cursor: 'pointer', fontWeight: 'bold', color: '#0c5460' }}>
            Security Best Practices
          </summary>
          <ul style={{ marginTop: '10px', color: '#0c5460' }}>
            <li>Never expose API keys in client-side code</li>
            <li>Use environment variables to store keys</li>
            <li>Rotate keys periodically</li>
            <li>Revoke keys immediately if compromised</li>
            <li>Use different keys for different environments</li>
            <li>Monitor API key usage regularly</li>
          </ul>
        </details>
      </div>
    )
  }