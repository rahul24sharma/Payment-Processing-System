import { useWebhookEndpoints, useDeleteWebhookEndpoint } from '@/hooks/useWebhooks'
import { formatDate } from '@/utils/formatters'
import type { WebhookEndpoint } from '@/types/webhook'

export default function WebhookEndpointsList() {
  const { data: endpoints, isLoading, error } = useWebhookEndpoints()
  const deleteEndpoint = useDeleteWebhookEndpoint()
  
  const handleDelete = async (id: string, url: string) => {
    if (!confirm(`Are you sure you want to delete webhook endpoint:\n\n${url}`)) {
      return
    }
    
    try {
      await deleteEndpoint.mutateAsync(id)
      alert('Webhook endpoint deleted successfully')
    } catch (error: any) {
      alert(`Error: ${error.response?.data?.error?.message || error.message}`)
    }
  }
  
  const handleCopySecret = (secret: string) => {
    navigator.clipboard.writeText(secret)
    alert('Secret copied to clipboard!')
  }
  
  if (isLoading) {
    return <div>Loading webhook endpoints...</div>
  }
  
  if (error) {
    return <div style={{ color: 'red' }}>Error: {error.message}</div>
  }
  
  if (!endpoints || endpoints.length === 0) {
    return (
      <div style={{
        textAlign: 'center',
        padding: '40px',
        border: '2px dashed #ddd',
        borderRadius: '8px',
      }}>
        <h3>No webhook endpoints configured</h3>
        <p style={{ color: '#666' }}>
          Create a webhook endpoint to receive real-time payment notifications
        </p>
      </div>
    )
  }
  
  return (
    <div>
      <div style={{ marginBottom: '20px' }}>
        <h3>Configured Endpoints ({endpoints.length})</h3>
      </div>
      
      {endpoints.map((endpoint: WebhookEndpoint) => (
        <div
          key={endpoint.id}
          style={{
            border: '1px solid #ddd',
            borderRadius: '8px',
            padding: '20px',
            marginBottom: '15px',
            background: endpoint.isActive ? 'white' : '#f5f5f5',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                <span style={{
                  padding: '4px 8px',
                  borderRadius: '4px',
                  background: endpoint.isActive ? '#28a745' : '#6c757d',
                  color: 'white',
                  fontSize: '12px',
                  fontWeight: 'bold',
                }}>
                  {endpoint.isActive ? 'ACTIVE' : 'INACTIVE'}
                </span>
                
                <code style={{
                  background: '#f5f5f5',
                  padding: '4px 8px',
                  borderRadius: '4px',
                  fontSize: '12px',
                }}>
                  {endpoint.id}
                </code>
              </div>
              
              <div style={{ marginBottom: '15px' }}>
                <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>URL:</div>
                <code style={{
                  background: '#f5f5f5',
                  padding: '8px 12px',
                  borderRadius: '4px',
                  display: 'block',
                  wordBreak: 'break-all',
                }}>
                  {endpoint.url}
                </code>
              </div>
              
              <div style={{ marginBottom: '15px' }}>
                <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>Signing Secret:</div>
                <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                  <code style={{
                    background: '#f5f5f5',
                    padding: '8px 12px',
                    borderRadius: '4px',
                    flex: 1,
                    fontFamily: 'monospace',
                  }}>
                    {endpoint.secret.substring(0, 20)}...
                  </code>
                  <button
                    onClick={() => handleCopySecret(endpoint.secret)}
                    style={{
                      padding: '8px 16px',
                      background: '#007bff',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                    }}
                  >
                    Copy
                  </button>
                </div>
              </div>
              
              <div style={{ marginBottom: '15px' }}>
                <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>
                  Subscribed Events ({endpoint.events.length}):
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                  {endpoint.events.map((event) => (
                    <span
                      key={event}
                      style={{
                        padding: '4px 12px',
                        background: '#e3f2fd',
                        color: '#1976d2',
                        borderRadius: '16px',
                        fontSize: '12px',
                        fontWeight: '500',
                      }}
                    >
                      {event}
                    </span>
                  ))}
                </div>
              </div>
              
              <div style={{ fontSize: '12px', color: '#666' }}>
                Created: {formatDate(endpoint.createdAt)}
              </div>
            </div>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <button
                onClick={() => handleDelete(endpoint.id, endpoint.url)}
                disabled={deleteEndpoint.isPending}
                style={{
                  padding: '8px 16px',
                  background: '#dc3545',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                }}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}