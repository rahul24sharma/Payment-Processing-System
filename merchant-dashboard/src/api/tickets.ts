import { apiClient } from './client'
import type {
  AddTicketCommentRequest,
  CreateTicketRequest,
  Ticket,
  UpdateTicketStatusRequest,
} from '@/types/ticket'

export const ticketsApi = {
  list: async (status?: string): Promise<Ticket[]> => {
    const response = await apiClient.get<Ticket[]>('/tickets', {
      params: status ? { status } : undefined,
    })
    return response.data
  },

  get: async (id: string): Promise<Ticket> => {
    const response = await apiClient.get<Ticket>(`/tickets/${id}`)
    return response.data
  },

  create: async (request: CreateTicketRequest): Promise<Ticket> => {
    const response = await apiClient.post<Ticket>('/tickets', request)
    return response.data
  },

  updateStatus: async (id: string, request: UpdateTicketStatusRequest): Promise<Ticket> => {
    const response = await apiClient.patch<Ticket>(`/tickets/${id}/status`, request)
    return response.data
  },

  addComment: async (id: string, request: AddTicketCommentRequest): Promise<Ticket> => {
    const response = await apiClient.post<Ticket>(`/tickets/${id}/comments`, request)
    return response.data
  },
}
