import { apiClient } from './client'
import type { WebhookEndpoint, Webhook, CreateWebhookEndpointRequest } from '@/types/webhook'

export const webhooksApi = {
  /**
   * List webhook endpoints
   */
  listEndpoints: async (): Promise<WebhookEndpoint[]> => {
    const response = await apiClient.get<WebhookEndpoint[]>('/webhooks/endpoints')
    return response.data
  },

  /**
   * Create webhook endpoint
   */
  createEndpoint: async (request: CreateWebhookEndpointRequest): Promise<WebhookEndpoint> => {
    const response = await apiClient.post<WebhookEndpoint>('/webhooks/endpoints', request)
    return response.data
  },

  /**
   * Delete webhook endpoint
   */
  deleteEndpoint: async (id: string): Promise<void> => {
    await apiClient.delete(`/webhooks/endpoints/${id}`)
  },

  /**
   * Get webhook delivery logs
   */
  getLogs: async (): Promise<Webhook[]> => {
    const response = await apiClient.get<Webhook[]>('/webhooks/logs')
    return response.data
  },

  /**
   * Test webhook endpoint
   */
  testEndpoint: async (id: string): Promise<void> => {
    await apiClient.post(`/webhooks/endpoints/${id}/test`)
  },
}
