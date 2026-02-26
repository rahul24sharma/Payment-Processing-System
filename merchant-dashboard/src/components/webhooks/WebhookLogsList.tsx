import { useWebhookLogs } from '@/hooks/useWebhooks'
import AsyncState from '@/components/ui/AsyncState'
import { formatDate } from '@/utils/formatters'
import type { Webhook } from '@/types/webhook'
import { useEffect, useId, useRef, useState } from 'react'
import './Webhooks.css'

export default function WebhookLogsList() {
  const { data: logs, isLoading, error } = useWebhookLogs()
  const [selectedLog, setSelectedLog] = useState<Webhook | null>(null)
  
  if (isLoading) {
    return <AsyncState kind="loading" title="Loading webhook deliveries" message="Fetching recent webhook delivery attempts and retry history." />
  }
  
  if (error) {
    return <AsyncState kind="error" title="Unable to load webhook deliveries" message={error.message} />
  }
  
  if (!logs || logs.length === 0) {
    return (
      <AsyncState
        kind="empty"
        title="No webhook deliveries yet"
        message="Webhook delivery logs will appear here after events are triggered."
      />
    )
  }
  
  return (
    <div className="webhook-logs">
      <div className="webhook-panel">
        <div className="webhook-panel__header">
          <div>
            <p className="webhook-panel__eyebrow">Delivery Activity</p>
            <h3>Recent deliveries</h3>
            <p>Auto-refreshes every 10 seconds</p>
          </div>
          <div className="webhook-panel__meta">{logs.length} logs</div>
        </div>

        <div className="webhook-logs__table-wrap">
          <table className="webhook-logs__table">
            <thead>
              <tr>
                <th>Event</th>
                <th>Status</th>
                <th>Attempts</th>
                <th>Response</th>
                <th>Time</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log: Webhook) => (
                <tr key={log.id}>
                  <td>
                    <div className="webhook-logs__event">
                      <div className="webhook-logs__event-type">{log.eventType}</div>
                      <div className="webhook-logs__subtle">
                        {log.url.length > 48 ? `${log.url.substring(0, 48)}...` : log.url}
                      </div>
                    </div>
                  </td>

                  <td>
                    <span className={`webhook-logs__status webhook-logs__status--${log.status.toLowerCase()}`}>
                      {log.status.toUpperCase()}
                    </span>
                  </td>

                  <td>
                    <div className="webhook-logs__attempts">{log.attempts} / 5</div>
                    {log.nextRetryAt && (
                      <div className="webhook-logs__next-retry">Next retry: {formatDate(log.nextRetryAt)}</div>
                    )}
                  </td>

                  <td>
                    {log.lastResponseCode ? (
                      <span
                        className={`webhook-logs__response-code ${
                          log.lastResponseCode >= 200 && log.lastResponseCode < 300
                            ? 'webhook-logs__response-code--success'
                            : 'webhook-logs__response-code--error'
                        }`}
                      >
                        {log.lastResponseCode}
                      </span>
                    ) : (
                      <span className="webhook-logs__subtle">N/A</span>
                    )}
                  </td>

                  <td className="webhook-logs__time-cell">
                    {formatDate(log.createdAt)}
                    {log.deliveredAt && (
                      <div className="webhook-logs__delivered-at">Delivered: {formatDate(log.deliveredAt)}</div>
                    )}
                  </td>

                  <td>
                    <button onClick={() => setSelectedLog(log)} className="webhook-logs__view-btn" type="button">
                      View Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

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
  const payload = safeFormatPayload(webhook.payload)
  const titleId = useId()
  const panelRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    panelRef.current?.focus()

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [onClose])

  return (
    <div className="webhook-modal" role="dialog" aria-modal="true" aria-labelledby={titleId}>
      <button className="webhook-modal__backdrop" onClick={onClose} aria-label="Close modal" type="button" />
      <div className="webhook-modal__panel" ref={panelRef} tabIndex={-1}>
        <div className="webhook-modal__header">
          <h2 id={titleId}>Webhook Details</h2>
          <button onClick={onClose} className="webhook-modal__close" type="button" aria-label="Close">
            ×
          </button>
        </div>

        <table className="webhook-modal__grid">
          <tbody>
            <tr>
              <td>Webhook ID:</td>
              <td><code className="webhook-modal__code">{webhook.id}</code></td>
            </tr>
            <tr>
              <td>Event Type:</td>
              <td>{webhook.eventType}</td>
            </tr>
            <tr>
              <td>URL:</td>
              <td style={{ wordBreak: 'break-all' }}>{webhook.url}</td>
            </tr>
            <tr>
              <td>Status:</td>
              <td>
                <span className={`webhook-modal__status webhook-modal__status--${webhook.status.toLowerCase()}`}>
                  {webhook.status.toUpperCase()}
                </span>
              </td>
            </tr>
            <tr>
              <td>Attempts:</td>
              <td>{webhook.attempts}</td>
            </tr>
            <tr>
              <td>Response Code:</td>
              <td>{webhook.lastResponseCode || 'N/A'}</td>
            </tr>
            <tr>
              <td>Created:</td>
              <td>{formatDate(webhook.createdAt)}</td>
            </tr>
            {webhook.deliveredAt && (
              <tr>
                <td>Delivered:</td>
                <td style={{ color: '#16a34a', fontWeight: 700 }}>{formatDate(webhook.deliveredAt)}</td>
              </tr>
            )}
            {webhook.lastError && (
              <tr>
                <td>Error:</td>
                <td style={{ color: '#dc2626' }}>{webhook.lastError}</td>
              </tr>
            )}
          </tbody>
        </table>

        <div>
          <h4 className="webhook-modal__section-title">Payload</h4>
          <pre className="webhook-modal__payload">{payload}</pre>
        </div>

        <button onClick={onClose} className="webhook-modal__footer-btn" type="button">
          Close
        </button>
      </div>
    </div>
  )
}

function safeFormatPayload(payload: string): string {
  try {
    return JSON.stringify(JSON.parse(payload), null, 2)
  } catch {
    return payload
  }
}
