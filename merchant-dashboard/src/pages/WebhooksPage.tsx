import { useState } from 'react'
import CreateWebhookForm from '@/components/webhooks/CreateWebhookForm'
import WebhookEndpointsList from '@/components/webhooks/WebhookEndpointsList'
import WebhookLogsList from '@/components/webhooks/WebhookLogsList'
import WebhookGuide from '@/components/webhooks/WebhookGuide'
import './WebhooksPage.css'

export default function WebhooksPage() {
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [activeTab, setActiveTab] = useState<'endpoints' | 'logs'>('endpoints')
  
  return (
    <div className="webhooks-page">
      <section className="webhooks-page__hero">
        <div>
          <p className="webhooks-page__eyebrow">Developer Integrations</p>
          <h1>Webhooks</h1>
          <p className="webhooks-page__subtitle">
            Configure secure endpoints, inspect delivery attempts, and troubleshoot real-time event notifications for
            payment lifecycle updates.
          </p>
        </div>
        <div className="webhooks-page__hero-card">
          <div className="webhooks-page__hero-card-label">Runtime Focus</div>
          <div className="webhooks-page__hero-card-value">
            {activeTab === 'endpoints' ? 'Endpoints' : 'Delivery Logs'}
          </div>
          <div className="webhooks-page__hero-card-note">Signatures, retries, and event visibility</div>
        </div>
      </section>

      <WebhookGuide />

      <div className="webhooks-page__tabs">
        <button
          onClick={() => setActiveTab('endpoints')}
          className={`webhooks-page__tab ${activeTab === 'endpoints' ? 'webhooks-page__tab--active' : ''}`}
          type="button"
        >
          Endpoints
        </button>

        <button
          onClick={() => setActiveTab('logs')}
          className={`webhooks-page__tab ${activeTab === 'logs' ? 'webhooks-page__tab--active' : ''}`}
          type="button"
        >
          Delivery Logs
        </button>
      </div>

      {activeTab === 'endpoints' && (
        <>
          <div className="webhooks-page__actions">
            <button
              onClick={() => setShowCreateForm(true)}
              className="webhooks-page__primary-btn"
              type="button"
            >
              + Add Webhook Endpoint
            </button>
          </div>

          <WebhookEndpointsList />
        </>
      )}

      {activeTab === 'logs' && <WebhookLogsList />}

      {showCreateForm && (
        <CreateWebhookForm onClose={() => setShowCreateForm(false)} />
      )}
    </div>
  )
}
