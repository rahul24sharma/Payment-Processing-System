import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ticketsApi } from '@/api/tickets'
import type { AddTicketCommentRequest, CreateTicketRequest, UpdateTicketStatusRequest } from '@/types/ticket'

export function useTickets(status?: string) {
  return useQuery({
    queryKey: ['tickets', status ?? 'all'],
    queryFn: () => ticketsApi.list(status),
    refetchInterval: (query) => {
      const rows = (query.state.data as any) ?? []
      if (!Array.isArray(rows) || rows.length === 0) {
        return 20000
      }
      const hasActiveTicket = rows.some((ticket: any) =>
        ticket?.status === 'OPEN' ||
        ticket?.status === 'IN_PROGRESS' ||
        ticket?.status === 'WAITING_ON_MERCHANT'
      )
      return hasActiveTicket ? 5000 : 20000
    },
    refetchIntervalInBackground: true,
  })
}

export function useTicket(id?: string) {
  return useQuery({
    queryKey: ['ticket', id],
    queryFn: () => ticketsApi.get(id as string),
    enabled: Boolean(id),
    refetchInterval: (query) => {
      const status = (query.state.data as any)?.status
      if (status === 'OPEN' || status === 'IN_PROGRESS' || status === 'WAITING_ON_MERCHANT') {
        return 4000
      }
      return 15000
    },
    refetchIntervalInBackground: true,
  })
}

export function useCreateTicket() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: CreateTicketRequest) => ticketsApi.create(request),
    onSuccess: (ticket) => {
      queryClient.setQueryData(['ticket', ticket.id], ticket)
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
    },
  })
}

export function useUpdateTicketStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: UpdateTicketStatusRequest }) =>
      ticketsApi.updateStatus(id, request),
    onSuccess: (ticket) => {
      queryClient.setQueryData(['ticket', ticket.id], ticket)
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
    },
  })
}

export function useAddTicketComment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: AddTicketCommentRequest }) =>
      ticketsApi.addComment(id, request),
    onSuccess: (ticket) => {
      queryClient.setQueryData(['ticket', ticket.id], ticket)
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
    },
  })
}
