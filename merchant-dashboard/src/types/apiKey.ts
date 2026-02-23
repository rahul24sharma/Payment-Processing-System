export interface ApiKey {
    id: string
    merchantId: string
    keyPrefix: string
    name: string
    isActive: boolean
    lastUsedAt?: string
    createdAt: string
    expiresAt?: string
    revokedAt?: string
    revokedBy?: string
  }
  
  export interface ApiKeyWithSecret extends ApiKey {
    key: string // Only available when first created
  }
  
  export interface CreateApiKeyRequest {
    name: string
    isLive: boolean
  }