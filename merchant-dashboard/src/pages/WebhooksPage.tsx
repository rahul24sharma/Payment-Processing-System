import { useState } from 'react'
import CreateWebhookForm from '@/components/webhooks/CreateWebhookForm'
import WebhookEndpointsList from '@/components/webhooks/WebhookEndpointsList'
import WebhookLogsList from '@/components/webhooks/WebhookLogsList'
import WebhookGuide from '@/components/webhooks/WebhookGuide'

export default function WebhooksPage() {
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [activeTab, setActiveTab] = useState<'endpoints' | 'logs'>('endpoints')
  
  return (
    <div style={{ margin: '20px' }}>
      <div style={{ marginBottom: '30px' }}>
        <h1 style={{ marginBottom: '10px' }}>Webhooks</h1>
        <p style={{ color: '#666', fontSize: '14px' }}>
          Configure webhook endpoints to receive real-time notifications about payment events
        </p>
      </div>
      
      {/* Guide Section */}
      <WebhookGuide />
      
      {/* Tabs */}
      <div style={{
        display: 'flex',
        borderBottom: '2px solid #ddd',
        marginBottom: '20px',
        gap: '20px',
      }}>
        <button
          onClick={() => setActiveTab('endpoints')}
          style={{
            padding: '10px 20px',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'endpoints' ? '3px solid #007bff' : 'none',
            color: activeTab === 'endpoints' ? '#007bff' : '#666',
            fontWeight: activeTab === 'endpoints' ? 'bold' : 'normal',
            cursor: 'pointer',
            marginBottom: '-2px',
          }}
        >
          Endpoints
        </button>
        
        <button
          onClick={() => setActiveTab('logs')}
          style={{
            padding: '10px 20px',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'logs' ? '3px solid #007bff' : 'none',
            color: activeTab === 'logs' ? '#007bff' : '#666',
            fontWeight: activeTab === 'logs' ? 'bold' : 'normal',
            cursor: 'pointer',
            marginBottom: '-2px',
          }}
        >
          Delivery Logs
        </button>
      </div>
      
      {/* Tab Content */}
      {activeTab === 'endpoints' && (
        <>
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
              + Add Webhook Endpoint
            </button>
          </div>
          
          <WebhookEndpointsList />
        </>
      )}
      
      {activeTab === 'logs' && <WebhookLogsList />}
      
      {/* Create Form Modal */}
      {showCreateForm && (
        <CreateWebhookForm onClose={() => setShowCreateForm(false)} />
      )}
    </div>
  )
}