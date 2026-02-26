import { apiClient } from './client'
import type { Payment, CreatePaymentRequest, RefundRequest, Refund } from '@/types/payment'

const LONG_PAYMENT_TIMEOUT_MS = 90_000

export const paymentsApi = {
  /**
   * Create a new payment
   */
  create: async (request: CreatePaymentRequest): Promise<Payment> => {
    const idempotencyKey = crypto.randomUUID()
    
    const response = await apiClient.post<Payment>('/payments', request, {
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
      timeout: LONG_PAYMENT_TIMEOUT_MS,
    })
    
    return response.data
  },

  /**
   * Get payment by ID
   */
  get: async (id: string): Promise<Payment> => {
    const response = await apiClient.get<Payment>(`/payments/${id}`)
    return response.data
  },

  /**
   * List payments
   */
  list: async (params?: {
    status?: string
    limit?: number
  }): Promise<{ data: Payment[]; hasMore: boolean; totalCount: number }> => {
    const response = await apiClient.get('/payments', { params })
    return response.data
  },

  /**
   * Capture an authorized payment
   */
  capture: async (id: string, amount?: number): Promise<Payment> => {
    const response = await apiClient.post<Payment>(`/payments/${id}/capture`, {
      amount,
    })
    return response.data
  },

  completeAuthentication: async (id: string): Promise<Payment> => {
    const response = await apiClient.post<Payment>(
      `/payments/${id}/complete-authentication`,
      undefined,
      { timeout: LONG_PAYMENT_TIMEOUT_MS }
    )
    return response.data
  },

  /**
   * Void an authorized payment
   */
  void: async (id: string): Promise<Payment> => {
    const response = await apiClient.post<Payment>(`/payments/${id}/void`)
    return response.data
  },

  /**
   * Create a refund
   */
  refund: async (id: string, request: RefundRequest): Promise<Refund> => {
    const response = await apiClient.post<Refund>(`/payments/${id}/refunds`, request)
    return response.data
  },
}
