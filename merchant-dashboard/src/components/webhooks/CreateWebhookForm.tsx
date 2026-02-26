import { useState } from 'react'
import { useCreateWebhookEndpoint } from '@/hooks/useWebhooks'
import { useToast } from '@/contexts/ToastContext'
import { AVAILABLE_EVENTS, EVENT_DESCRIPTIONS } from '@/types/webhook'
import './Webhooks.css'

export default function CreateWebhookForm({ onClose }: { onClose: () => void }) {
  const [url, setUrl] = useState('')
  const [selectedEvents, setSelectedEvents] = useState<string[]>(['PAYMENT_CAPTURED'])
  const toast = useToast()
  
  const createEndpoint = useCreateWebhookEndpoint()
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!url.startsWith('https://')) {
      toast.error('Webhook URL must use HTTPS for security')
      return
    }
    
    if (selectedEvents.length === 0) {
      toast.error('Please select at least one event')
      return
    }
    
    try {
      const endpoint = await createEndpoint.mutateAsync({
        url,
        events: selectedEvents,
      })
      
      toast.success(
        `Webhook created.\nSecret: ${endpoint.secret}\nSave this secret - you'll need it to verify webhook signatures.`,
        { title: 'Webhook Endpoint Created', durationMs: 10000 },
      )
      
      setUrl('')
      setSelectedEvents(['PAYMENT_CAPTURED'])
      onClose()
    } catch (error: any) {
      toast.error(error.response?.data?.error?.message || error.message)
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
    <div className="webhook-modal" role="dialog" aria-modal="true" aria-label="Create webhook endpoint">
      <button className="webhook-modal__backdrop" onClick={onClose} aria-label="Close modal" type="button" />
      <div className="webhook-modal__panel">
        <div className="webhook-modal__header">
          <h2>Create Webhook Endpoint</h2>
          <button onClick={onClose} className="webhook-modal__close" type="button" aria-label="Close">
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="webhook-form">
          <div className="webhook-form__field">
            <label htmlFor="webhook-url">Webhook URL *</label>
            <input
              id="webhook-url"
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://your-domain.com/webhooks"
              required
            />
            <small className="webhook-form__hint">Must be HTTPS. We'll send POST requests to this URL.</small>
          </div>

          <div className="webhook-form__field">
            <span>Events to Subscribe *</span>
            <div className="webhook-form__events">
              {AVAILABLE_EVENTS.map((event) => (
                <div
                  key={event}
                  className={`webhook-form__event ${selectedEvents.includes(event) ? 'webhook-form__event--selected' : ''}`}
                  onClick={() => toggleEvent(event)}
                >
                  <label className="webhook-form__event-label">
                    <input
                      type="checkbox"
                      checked={selectedEvents.includes(event)}
                      onChange={() => toggleEvent(event)}
                    />
                    <div>
                      <div className="webhook-form__event-title">{event}</div>
                      <div className="webhook-form__event-desc">{EVENT_DESCRIPTIONS[event]}</div>
                    </div>
                  </label>
                </div>
              ))}
            </div>
            <small className="webhook-form__hint">
              Select which events you want to receive
            </small>
          </div>

          <div className="webhook-form__warning">
            <strong>Important:</strong> After creating the webhook, you'll receive a secret key.
            Save it securely - you'll need it to verify webhook signatures.
          </div>

          <div className="webhook-form__actions">
            <button
              type="submit"
              disabled={createEndpoint.isPending}
              className="webhook-form__submit"
            >
              {createEndpoint.isPending ? 'Creating...' : 'Create Webhook'}
            </button>

            <button
              type="button"
              onClick={onClose}
              className="webhook-form__cancel"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
