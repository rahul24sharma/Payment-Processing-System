import { useWebhookEndpoints, useDeleteWebhookEndpoint } from '@/hooks/useWebhooks'
import { useToast } from '@/contexts/ToastContext'
import ConfirmDialog from '@/components/ui/ConfirmDialog'
import AsyncState from '@/components/ui/AsyncState'
import { formatDate } from '@/utils/formatters'
import type { WebhookEndpoint } from '@/types/webhook'
import { useState } from 'react'
import './Webhooks.css'

export default function WebhookEndpointsList() {
  const { data: endpoints, isLoading, error } = useWebhookEndpoints()
  const deleteEndpoint = useDeleteWebhookEndpoint()
  const toast = useToast()
  const [pendingDelete, setPendingDelete] = useState<{ id: string; url: string } | null>(null)
  
  const handleDelete = async (id: string) => {
    try {
      await deleteEndpoint.mutateAsync(id)
      toast.success('Webhook endpoint deleted successfully')
      setPendingDelete(null)
    } catch (error: any) {
      toast.error(error.response?.data?.error?.message || error.message)
    }
  }
  
  const handleCopySecret = (secret: string) => {
    navigator.clipboard.writeText(secret)
    toast.success('Secret copied to clipboard!')
  }
  
  if (isLoading) {
    return <AsyncState kind="loading" title="Loading webhook endpoints" message="Fetching endpoint configuration and signing secrets." />
  }
  
  if (error) {
    return <AsyncState kind="error" title="Unable to load webhook endpoints" message={error.message} />
  }
  
  if (!endpoints || endpoints.length === 0) {
    return (
      <AsyncState
        kind="empty"
        title="No webhook endpoints configured"
        message="Create a webhook endpoint to receive real-time payment lifecycle notifications."
      />
    )
  }
  
  return (
    <div className="webhook-endpoints">
      <div className="webhook-panel">
        <div className="webhook-panel__header">
          <div>
            <p className="webhook-panel__eyebrow">Configured Endpoints</p>
            <h3>Webhook destinations</h3>
            <p>Manage endpoint URLs, event subscriptions, and signing secrets.</p>
          </div>
          <div className="webhook-panel__meta">{endpoints.length} endpoints</div>
        </div>

        <div className="webhook-endpoints__list" style={{ padding: '12px' }}>
          {endpoints.map((endpoint: WebhookEndpoint) => (
            <div
              key={endpoint.id}
              className={`webhook-endpoints__card ${endpoint.isActive ? '' : 'webhook-endpoints__card--inactive'}`}
            >
              <div className="webhook-endpoints__top">
                <div className="webhook-endpoints__main">
                  <div className="webhook-endpoints__chips">
                    <span
                      className={`webhook-endpoints__status ${
                        endpoint.isActive ? 'webhook-endpoints__status--active' : 'webhook-endpoints__status--inactive'
                      }`}
                    >
                      {endpoint.isActive ? 'ACTIVE' : 'INACTIVE'}
                    </span>

                    <code className="webhook-endpoints__id">{endpoint.id}</code>
                  </div>

                  <div>
                    <p className="webhook-endpoints__label">URL</p>
                    <code className="webhook-endpoints__code-block">{endpoint.url}</code>
                  </div>

                  <div>
                    <p className="webhook-endpoints__label">Signing Secret</p>
                    <div className="webhook-endpoints__secret-row">
                      <code className="webhook-endpoints__code-block">{endpoint.secret.substring(0, 20)}...</code>
                      <button
                        onClick={() => handleCopySecret(endpoint.secret)}
                        className="webhook-endpoints__copy-btn"
                        type="button"
                      >
                        Copy Secret
                      </button>
                    </div>
                  </div>

                  <div>
                    <p className="webhook-endpoints__label">Subscribed Events ({endpoint.events.length})</p>
                    <div className="webhook-endpoints__event-list">
                      {endpoint.events.map((event) => (
                        <span key={event} className="webhook-endpoints__event">
                          {event}
                        </span>
                      ))}
                    </div>
                  </div>

                  <div className="webhook-endpoints__created">Created: {formatDate(endpoint.createdAt)}</div>
                </div>

                <div>
                  <button
                    onClick={() => setPendingDelete({ id: endpoint.id, url: endpoint.url })}
                    disabled={deleteEndpoint.isPending}
                    className="webhook-endpoints__delete-btn"
                    type="button"
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <ConfirmDialog
        open={!!pendingDelete}
        title="Delete Webhook Endpoint"
        message={
          pendingDelete ? (
            <p>
              Are you sure you want to delete this endpoint?
              <br />
              <code>{pendingDelete.url}</code>
            </p>
          ) : null
        }
        confirmLabel="Delete Endpoint"
        tone="danger"
        busy={deleteEndpoint.isPending}
        onConfirm={() => pendingDelete && handleDelete(pendingDelete.id)}
        onClose={() => setPendingDelete(null)}
      />
    </div>
  )
}
