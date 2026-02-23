import { useState } from 'react'
import { useCreateWebhookEndpoint } from '@/hooks/useWebhooks'
import { AVAILABLE_EVENTS, EVENT_DESCRIPTIONS } from '@/types/webhook'

export default function CreateWebhookForm({ onClose }: { onClose: () => void }) {
  const [url, setUrl] = useState('')
  const [selectedEvents, setSelectedEvents] = useState<string[]>(['PAYMENT_CAPTURED'])
  
  const createEndpoint = useCreateWebhookEndpoint()
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!url.startsWith('https://')) {
      alert('Webhook URL must use HTTPS for security')
      return
    }
    
    if (selectedEvents.length === 0) {
      alert('Please select at least one event')
      return
    }
    
    try {
      const endpoint = await createEndpoint.mutateAsync({
        url,
        events: selectedEvents,
      })
      
      alert(`Webhook created!\n\nSecret: ${endpoint.secret}\n\nSave this secret - you'll need it to verify webhook signatures.`)
      
      setUrl('')
      setSelectedEvents(['PAYMENT_CAPTURED'])
      onClose()
    } catch (error: any) {
      alert(`Error: ${error.response?.data?.error?.message || error.message}`)
    }
  }
  
  const toggleEvent = (event: string) => {
    if (selectedEvents.includes(event)) {
      setSelectedEvents(selectedEvents.filter((e) => e !== event))
    } else {
      setSelectedEvents([...selectedEvents, event])
    }
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
        maxHeight: '80vh',
        overflow: 'auto',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
          <h2 style={{ margin: 0 }}>Create Webhook Endpoint</h2>
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
              Webhook URL *
            </label>
            <input
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://your-domain.com/webhooks"
              required
              style={{
                width: '100%',
                padding: '10px',
                border: '1px solid #ddd',
                borderRadius: '4px',
              }}
            />
            <small style={{ color: '#666', fontSize: '12px' }}>
              Must be HTTPS. We'll send POST requests to this URL.
            </small>
          </div>
          
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', marginBottom: '10px', fontWeight: 'bold' }}>
              Events to Subscribe *
            </label>
            <div style={{ 
              border: '1px solid #ddd', 
              borderRadius: '4px', 
              padding: '15px',
              maxHeight: '300px',
              overflow: 'auto',
            }}>
              {AVAILABLE_EVENTS.map((event) => (
                <div
                  key={event}
                  style={{
                    marginBottom: '10px',
                    padding: '10px',
                    background: selectedEvents.includes(event) ? '#e3f2fd' : '#f5f5f5',
                    borderRadius: '4px',
                    cursor: 'pointer',
                  }}
                  onClick={() => toggleEvent(event)}
                >
                  <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={selectedEvents.includes(event)}
                      onChange={() => toggleEvent(event)}
                      style={{ marginRight: '10px' }}
                    />
                    <div>
                      <div style={{ fontWeight: 'bold' }}>{event}</div>
                      <div style={{ fontSize: '12px', color: '#666' }}>
                        {EVENT_DESCRIPTIONS[event]}
                      </div>
                    </div>
                  </label>
                </div>
              ))}
            </div>
            <small style={{ color: '#666', fontSize: '12px', display: 'block', marginTop: '5px' }}>
              Select which events you want to receive
            </small>
          </div>
          
          <div style={{
            background: '#fff3cd',
            padding: '15px',
            borderRadius: '4px',
            marginBottom: '20px',
            fontSize: '14px',
          }}>
            <strong>Important:</strong> After creating the webhook, you'll receive a secret key.
            Save it securely - you'll need it to verify webhook signatures.
          </div>
          
          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              type="submit"
              disabled={createEndpoint.isPending}
              style={{
                flex: 1,
                padding: '12px',
                background: createEndpoint.isPending ? '#ccc' : '#28a745',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: createEndpoint.isPending ? 'not-allowed' : 'pointer',
                fontWeight: 'bold',
              }}
            >
              {createEndpoint.isPending ? 'Creating...' : 'Create Webhook'}
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
      </div>
    </div>
  )
}