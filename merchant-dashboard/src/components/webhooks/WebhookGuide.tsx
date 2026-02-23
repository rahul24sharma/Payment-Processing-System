export default function WebhookGuide() {
    return (
      <div style={{
        border: '1px solid #d1ecf1',
        background: '#d1ecf1',
        padding: '20px',
        borderRadius: '8px',
        marginBottom: '30px',
      }}>
        <h3 style={{ marginTop: 0, color: '#0c5460' }}>How Webhooks Work</h3>
        
        <ol style={{ marginBottom: '15px', color: '#0c5460' }}>
          <li style={{ marginBottom: '8px' }}>
            <strong>Configure an endpoint</strong> - Provide an HTTPS URL where you want to receive notifications
          </li>
          <li style={{ marginBottom: '8px' }}>
            <strong>Select events</strong> - Choose which payment events to subscribe to
          </li>
          <li style={{ marginBottom: '8px' }}>
            <strong>Verify signatures</strong> - Use the webhook secret to verify requests are from us
          </li>
          <li style={{ marginBottom: '8px' }}>
            <strong>Return 200 OK</strong> - Respond quickly to acknowledge receipt
          </li>
        </ol>
        
        <details style={{ marginTop: '15px' }}>
          <summary style={{ cursor: 'pointer', fontWeight: 'bold', color: '#0c5460' }}>
            Example: Verify Webhook Signature (Node.js)
          </summary>
          <pre style={{
            background: '#fff',
            padding: '15px',
            borderRadius: '4px',
            overflow: 'auto',
            marginTop: '10px',
            fontSize: '12px',
          }}>
  {`const crypto = require('crypto');
  
  app.post('/webhooks', (req, res) => {
    const signature = req.headers['x-webhook-signature'];
    const webhookId = req.headers['x-webhook-id'];
    const timestamp = req.headers['x-webhook-timestamp'];
    const payload = JSON.stringify(req.body);
    
    // Reconstruct signed payload
    const signedPayload = \`\${webhookId}.\${timestamp}.\${payload}\`;
    
    // Compute HMAC
    const hmac = crypto.createHmac('sha256', WEBHOOK_SECRET);
    hmac.update(signedPayload);
    const computed = 'sha256=' + hmac.digest('hex');
    
    // Verify signature
    if (signature !== computed) {
      return res.status(401).send('Invalid signature');
    }
    
    // Process webhook
    console.log('Payment event:', req.body);
    
    // Acknowledge receipt
    res.status(200).send('OK');
  });`}
          </pre>
        </details>
        
        <div style={{ marginTop: '15px', padding: '10px', background: '#fff3cd', borderRadius: '4px' }}>
          <strong>⚠️ Important:</strong> Always respond with 200 OK within 5 seconds. 
          Process webhooks asynchronously to avoid timeouts.
        </div>
      </div>
    )
  }