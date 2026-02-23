import { apiClient } from './client'
import type { ApiKey, ApiKeyWithSecret, CreateApiKeyRequest } from '@/types/apiKey'

export const apiKeysApi = {
  /**
   * List all API keys
   */
  list: async (): Promise<ApiKey[]> => {
    const response = await apiClient.get<ApiKey[]>('/api-keys')
    return response.data
  },

  /**
   * Generate new API key
   */
  create: async (request: CreateApiKeyRequest): Promise<ApiKeyWithSecret> => {
    const response = await apiClient.post<ApiKeyWithSecret>('/api-keys', request)
    return response.data
  },

  /**
   * Revoke API key
   */
  revoke: async (id: string): Promise<void> => {
    await apiClient.delete(`/api-keys/${id}`)
  },
}