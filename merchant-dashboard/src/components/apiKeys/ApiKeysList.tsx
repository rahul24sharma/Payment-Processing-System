import { useApiKeys, useRevokeApiKey } from '@/hooks/useApiKeys'
import { formatDate } from '@/utils/formatters'
import type{ ApiKey } from '@/types/apiKey'

export default function ApiKeysList() {
  const { data: apiKeys, isLoading, error } = useApiKeys()
  const revokeKey = useRevokeApiKey()
  
  const handleRevoke = async (id: string, name: string) => {
    if (!confirm(`Are you sure you want to revoke the API key:\n\n"${name}"\n\nThis action cannot be undone.`)) {
      return
    }
    
    try {
      await revokeKey.mutateAsync(id)
      alert('API key revoked successfully')
    } catch (error: any) {
      alert(`Error: ${error.response?.data?.error?.message || error.message}`)
    }
  }
  
  if (isLoading) {
    return <div>Loading API keys...</div>
  }
  
  if (error) {
    return <div style={{ color: 'red' }}>Error: {error.message}</div>
  }
  
  const activeKeys = apiKeys?.filter((key: ApiKey) => key.isActive) || []
  const revokedKeys = apiKeys?.filter((key: ApiKey) => !key.isActive) || []
  
  return (
    <div>
      {/* Active Keys */}
      <div style={{ marginBottom: '30px' }}>
        <h3>Active API Keys ({activeKeys.length})</h3>
        
        {activeKeys.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: '40px',
            border: '2px dashed #ddd',
            borderRadius: '8px',
          }}>
            <h4>No active API keys</h4>
            <p style={{ color: '#666' }}>
              Generate an API key to start accepting payments
            </p>
          </div>
        ) : (
          <div>
            {activeKeys.map((apiKey: ApiKey) => (
              <div
                key={apiKey.id}
                style={{
                  border: '1px solid #ddd',
                  borderRadius: '8px',
                  padding: '20px',
                  marginBottom: '15px',
                  background: 'white',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                      <h4 style={{ margin: 0 }}>{apiKey.name || 'Unnamed Key'}</h4>
                      
                      <span style={{
                        padding: '4px 8px',
                        borderRadius: '4px',
                        background: apiKey.keyPrefix.includes('live') ? '#dc3545' : '#007bff',
                        color: 'white',
                        fontSize: '12px',
                        fontWeight: 'bold',
                      }}>
                        {apiKey.keyPrefix.includes('live') ? 'LIVE' : 'TEST'}
                      </span>
                    </div>
                    
                    <div style={{ marginBottom: '10px' }}>
                      <code style={{
                        background: '#f5f5f5',
                        padding: '8px 12px',
                        borderRadius: '4px',
                        fontSize: '14px',
                        fontFamily: 'monospace',
                      }}>
                        {apiKey.keyPrefix}••••••••••••••••
                      </code>
                    </div>
                    
                    <div style={{ display: 'flex', gap: '20px', fontSize: '12px', color: '#666' }}>
                      <div>
                        <strong>Created:</strong> {formatDate(apiKey.createdAt)}
                      </div>
                      {apiKey.lastUsedAt && (
                        <div>
                          <strong>Last used:</strong> {formatDate(apiKey.lastUsedAt)}
                        </div>
                      )}
                      {!apiKey.lastUsedAt && (
                        <div style={{ color: '#ffc107' }}>
                          <strong>Never used</strong>
                        </div>
                      )}
                    </div>
                  </div>
                  
                  <button
                    onClick={() => handleRevoke(apiKey.id, apiKey.name)}
                    disabled={revokeKey.isPending}
                    style={{
                      padding: '8px 16px',
                      background: '#dc3545',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                    }}
                  >
                    Revoke
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
      
      {/* Revoked Keys */}
      {revokedKeys.length > 0 && (
        <div>
          <h3>Revoked API Keys ({revokedKeys.length})</h3>
          
          {revokedKeys.map((apiKey: ApiKey) => (
            <div
              key={apiKey.id}
              style={{
                border: '1px solid #ddd',
                borderRadius: '8px',
                padding: '20px',
                marginBottom: '15px',
                background: '#f5f5f5',
                opacity: 0.7,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                <h4 style={{ margin: 0 }}>{apiKey.name || 'Unnamed Key'}</h4>
                <span style={{
                  padding: '4px 8px',
                  borderRadius: '4px',
                  background: '#6c757d',
                  color: 'white',
                  fontSize: '12px',
                }}>
                  REVOKED
                </span>
              </div>
              
              <div style={{ fontSize: '12px', color: '#666' }}>
                Revoked: {apiKey.revokedAt ? formatDate(apiKey.revokedAt) : 'Unknown'}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}