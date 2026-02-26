import { useApiKeys, useRevokeApiKey } from '@/hooks/useApiKeys'
import { useToast } from '@/contexts/ToastContext'
import ConfirmDialog from '@/components/ui/ConfirmDialog'
import AsyncState from '@/components/ui/AsyncState'
import { formatDate } from '@/utils/formatters'
import type{ ApiKey } from '@/types/apiKey'
import { useState } from 'react'
import './ApiKeys.css'

export default function ApiKeysList() {
  const { data: apiKeys, isLoading, error } = useApiKeys()
  const revokeKey = useRevokeApiKey()
  const toast = useToast()
  const [pendingRevoke, setPendingRevoke] = useState<{ id: string; name: string } | null>(null)
  
  const handleRevoke = async (id: string) => {
    try {
      await revokeKey.mutateAsync(id)
      toast.success('API key revoked successfully')
      setPendingRevoke(null)
    } catch (error: any) {
      toast.error(error.response?.data?.error?.message || error.message)
    }
  }
  
  if (isLoading) {
    return <AsyncState kind="loading" title="Loading API keys" message="Fetching active and revoked keys for this workspace." />
  }
  
  if (error) {
    return <AsyncState kind="error" title="Unable to load API keys" message={error.message} />
  }
  
  const activeKeys = apiKeys?.filter((key: ApiKey) => key.isActive) || []
  const revokedKeys = apiKeys?.filter((key: ApiKey) => !key.isActive) || []
  
  return (
    <div className="api-keys-list">
      <section className="api-keys-list__section">
        <div className="api-keys-panel">
          <div className="api-keys-panel__header">
            <div>
              <p className="api-keys-panel__eyebrow">Active Keys</p>
              <h3>Server secrets in use</h3>
              <p>Revoke immediately if a key is exposed or no longer needed.</p>
            </div>
            <div className="api-keys-panel__meta">{activeKeys.length} active</div>
          </div>

          <div style={{ padding: '12px' }}>
        {activeKeys.length === 0 ? (
          <AsyncState
            kind="empty"
            compact
            title="No active API keys"
            message="Generate an API key to start accepting payments from your backend."
          />
        ) : (
          <div className="api-keys-list__cards">
            {activeKeys.map((apiKey: ApiKey) => (
              <div key={apiKey.id} className="api-keys-list__card">
                <div className="api-keys-list__row">
                  <div className="api-keys-list__main">
                    <div className="api-keys-list__title-row">
                      <h4 className="api-keys-list__title">{apiKey.name || 'Unnamed Key'}</h4>
                      <span
                        className={`api-keys-list__mode ${
                          apiKey.keyPrefix.includes('live') ? 'api-keys-list__mode--live' : 'api-keys-list__mode--test'
                        }`}
                      >
                        {apiKey.keyPrefix.includes('live') ? 'LIVE' : 'TEST'}
                      </span>
                    </div>

                    <div>
                      <code className="api-keys-list__keycode">{apiKey.keyPrefix}••••••••••••••••</code>
                    </div>

                    <div className="api-keys-list__meta-grid">
                      <div>
                        <strong>Created:</strong> {formatDate(apiKey.createdAt)}
                      </div>
                      {apiKey.lastUsedAt && (
                        <div>
                          <strong>Last used:</strong> {formatDate(apiKey.lastUsedAt)}
                        </div>
                      )}
                      {!apiKey.lastUsedAt && (
                        <div className="api-keys-list__never-used">
                          Never used
                        </div>
                      )}
                    </div>
                  </div>

                  <button
                    onClick={() => setPendingRevoke({ id: apiKey.id, name: apiKey.name })}
                    disabled={revokeKey.isPending}
                    className="api-keys-list__revoke-btn"
                    type="button"
                  >
                    Revoke
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
          </div>
        </div>
      </section>

      {revokedKeys.length > 0 && (
        <section className="api-keys-list__section">
          <div className="api-keys-panel">
            <div className="api-keys-panel__header">
              <div>
                <p className="api-keys-panel__eyebrow">Revoked Keys</p>
                <h3>Retired credentials</h3>
                <p>Keep these visible for audits and incident review.</p>
              </div>
              <div className="api-keys-panel__meta">{revokedKeys.length} revoked</div>
            </div>

            <div className="api-keys-list__cards" style={{ padding: '12px' }}>
              {revokedKeys.map((apiKey: ApiKey) => (
                <div key={apiKey.id} className="api-keys-list__card api-keys-list__card--revoked">
                  <div className="api-keys-list__main">
                    <div className="api-keys-list__title-row">
                      <h4 className="api-keys-list__title">{apiKey.name || 'Unnamed Key'}</h4>
                      <span className="api-keys-list__mode api-keys-list__mode--revoked">REVOKED</span>
                    </div>
                    <div className="api-keys-list__meta-grid">
                      <div>Revoked: {apiKey.revokedAt ? formatDate(apiKey.revokedAt) : 'Unknown'}</div>
                      <div>Created: {formatDate(apiKey.createdAt)}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      <ConfirmDialog
        open={!!pendingRevoke}
        title="Revoke API Key"
        message={
          pendingRevoke ? (
            <p>
              Are you sure you want to revoke the API key <strong>"{pendingRevoke.name}"</strong>? This action cannot
              be undone.
            </p>
          ) : null
        }
        confirmLabel="Revoke Key"
        tone="danger"
        busy={revokeKey.isPending}
        onConfirm={() => pendingRevoke && handleRevoke(pendingRevoke.id)}
        onClose={() => setPendingRevoke(null)}
      />
    </div>
  )
}
