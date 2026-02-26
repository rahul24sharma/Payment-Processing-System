import './Webhooks.css'

export default function WebhookGuide() {
  return (
    <section className="webhook-guide">
      <div className="webhook-guide__header">
        <h3>How Webhooks Work</h3>
        <p>Endpoints receive signed POST requests for subscribed payment events. Acknowledge quickly and process safely.</p>
      </div>

      <ol className="webhook-guide__steps">
        <li>
          <strong>Configure an endpoint</strong> - Provide an HTTPS URL where you want to receive notifications
        </li>
        <li>
          <strong>Select events</strong> - Choose which payment events to subscribe to
        </li>
        <li>
          <strong>Verify signatures</strong> - Use the webhook secret to verify requests are from us
        </li>
        <li>
          <strong>Return 200 OK</strong> - Respond quickly to acknowledge receipt
        </li>
      </ol>

      <details className="webhook-guide__details">
        <summary>Example: Verify Webhook Signature (Node.js)</summary>
        <pre className="webhook-guide__code">
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

      <div className="webhook-guide__warning">
        <strong>Important:</strong> Always respond with `200 OK` within 5 seconds. Process webhook jobs asynchronously to
        avoid retries and delivery timeouts.
      </div>
    </section>
  )
}
