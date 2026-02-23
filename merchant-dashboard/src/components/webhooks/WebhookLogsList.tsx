import { useWebhookLogs } from '@/hooks/useWebhooks'
import { formatDate } from '@/utils/formatters'
import type { Webhook } from '@/types/webhook'
import { useState } from 'react'

export default function WebhookLogsList() {
  const { data: logs, isLoading, error } = useWebhookLogs()
  const [selectedLog, setSelectedLog] = useState<Webhook | null>(null)
  
  if (isLoading) {
    return <div>Loading webhook logs...</div>
  }
  
  if (error) {
    return <div style={{ color: 'red' }}>Error: {error.message}</div>
  }
  
  if (!logs || logs.length === 0) {
    return (
      <div style={{
        textAlign: 'center',
        padding: '40px',
        border: '2px dashed #ddd',
        borderRadius: '8px',
      }}>
        <h3>No webhook deliveries yet</h3>
        <p style={{ color: '#666' }}>
          Webhook delivery logs will appear here when events are triggered
        </p>
      </div>
    )
  }
  
  return (
    <div>
      <div style={{ marginBottom: '20px' }}>
        <h3>Recent Deliveries ({logs.length})</h3>
        <small style={{ color: '#666' }}>Auto-refreshes every 10 seconds</small>
      </div>
      
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ background: '#f5f5f5' }}>
            <th style={{ padding: '12px', textAlign: 'left', border: '1px solid #ddd' }}>Event</th>
            <th style={{ padding: '12px', textAlign: 'left', border: '1px solid #ddd' }}>Status</th>
            <th style={{ padding: '12px', textAlign: 'left', border: '1px solid #ddd' }}>Attempts</th>
            <th style={{ padding: '12px', textAlign: 'left', border: '1px solid #ddd' }}>Response</th>
            <th style={{ padding: '12px', textAlign: 'left', border: '1px solid #ddd' }}>Time</th>
            <th style={{ padding: '12px', textAlign: 'left', border: '1px solid #ddd' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {logs.map((log: Webhook) => (
            <tr key={log.id} style={{ borderBottom: '1px solid #ddd' }}>
              <td style={{ padding: '12px', border: '1px solid #ddd' }}>
                <div style={{ fontWeight: 'bold' }}>{log.eventType}</div>
                <div style={{ fontSize: '12px', color: '#666' }}>
                  {log.url.length > 40 ? log.url.substring(0, 40) + '...' : log.url}
                </div>
              </td>
              
              <td style={{ padding: '12px', border: '1px solid #ddd' }}>
                <span style={{
                  padding: '4px 8px',
                  borderRadius: '4px',
                  background: getStatusColor(log.status),
                  color: 'white',
                  fontSize: '12px',
                  fontWeight: 'bold',
                }}>
                  {log.status.toUpperCase()}
                </span>
              </td>
              
              <td style={{ padding: '12px', border: '1px solid #ddd' }}>
                {log.attempts} / 5
                {log.nextRetryAt && (
                  <div style={{ fontSize: '12px', color: '#666' }}>
                    Next retry: {formatDate(log.nextRetryAt)}
                  </div>
                )}
              </td>
              
              <td style={{ padding: '12px', border: '1px solid #ddd' }}>
                {log.lastResponseCode && (
                  <span style={{
                    padding: '4px 8px',
                    borderRadius: '4px',
                    background: log.lastResponseCode >= 200 && log.lastResponseCode < 300 
                      ? '#d4edda' : '#f8d7da',
                    fontSize: '12px',
                  }}>
                    {log.lastResponseCode}
                  </span>
                )}
              </td>
              
              <td style={{ padding: '12px', border: '1px solid #ddd', fontSize: '12px' }}>
                {formatDate(log.createdAt)}
                {log.deliveredAt && (
                  <div style={{ color: '#28a745' }}>
                    ✓ Delivered: {formatDate(log.deliveredAt)}
                  </div>
                )}
              </td>
              
              <td style={{ padding: '12px', border: '1px solid #ddd' }}>
                <button
                  onClick={() => setSelectedLog(log)}
                  style={{
                    padding: '6px 12px',
                    background: '#007bff',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '12px',
                  }}
                >
                  View Details
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      
      {/* Webhook Detail Modal */}
      {selectedLog && (
        <WebhookDetailModal 
          webhook={selectedLog} 
          onClose={() => setSelectedLog(null)} 
        />
      )}
    </div>
  )
}

function WebhookDetailModal({ webhook, onClose }: { webhook: Webhook; onClose: () => void }) {
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
        maxWidth: '700px',
        width: '100%',
        maxHeight: '80vh',
        overflow: 'auto',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
          <h2 style={{ margin: 0 }}>Webhook Details</h2>
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
        
        <table style={{ width: '100%', marginBottom: '20px' }}>
          <tbody>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold', width: '150px' }}>Webhook ID:</td>
              <td style={{ padding: '8px' }}><code>{webhook.id}</code></td>
            </tr>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>Event Type:</td>
              <td style={{ padding: '8px' }}>{webhook.eventType}</td>
            </tr>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>URL:</td>
              <td style={{ padding: '8px', wordBreak: 'break-all' }}>{webhook.url}</td>
            </tr>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>Status:</td>
              <td style={{ padding: '8px' }}>
                <span style={{
                  padding: '4px 8px',
                  borderRadius: '4px',
                  background: getStatusColor(webhook.status),
                  color: 'white',
                }}>
                  {webhook.status.toUpperCase()}
                </span>
              </td>
            </tr>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>Attempts:</td>
              <td style={{ padding: '8px' }}>{webhook.attempts}</td>
            </tr>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>Response Code:</td>
              <td style={{ padding: '8px' }}>{webhook.lastResponseCode || 'N/A'}</td>
            </tr>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>Created:</td>
              <td style={{ padding: '8px' }}>{formatDate(webhook.createdAt)}</td>
            </tr>
            {webhook.deliveredAt && (
              <tr>
                <td style={{ padding: '8px', fontWeight: 'bold' }}>Delivered:</td>
                <td style={{ padding: '8px', color: '#28a745' }}>
                  ✓ {formatDate(webhook.deliveredAt)}
                </td>
              </tr>
            )}
            {webhook.lastError && (
              <tr>
                <td style={{ padding: '8px', fontWeight: 'bold', verticalAlign: 'top' }}>Error:</td>
                <td style={{ padding: '8px', color: '#dc3545' }}>{webhook.lastError}</td>
              </tr>
            )}
          </tbody>
        </table>
        
        <div style={{ marginBottom: '20px' }}>
          <div style={{ fontWeight: 'bold', marginBottom: '10px' }}>Payload:</div>
          <pre style={{
            background: '#f5f5f5',
            padding: '15px',
            borderRadius: '4px',
            overflow: 'auto',
            maxHeight: '300px',
            fontSize: '12px',
          }}>
            {JSON.stringify(JSON.parse(webhook.payload), null, 2)}
          </pre>
        </div>
        
        <button
          onClick={onClose}
          style={{
            width: '100%',
            padding: '12px',
            background: '#6c757d',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
          }}
        >
          Close
        </button>
      </div>
    </div>
  )
}

function getStatusColor(status: string): string {
  switch (status.toLowerCase()) {
    case 'delivered':
      return '#28a745'
    case 'pending':
      return '#ffc107'
    case 'failed':
      return '#dc3545'
    default:
      return '#6c757d'
  }
}