import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { paymentsApi } from '@/api/payments'
import type { CreatePaymentRequest, RefundRequest } from '@/types/payment'

function updatePaymentInListCaches(queryClient: ReturnType<typeof useQueryClient>, payment: any) {
  queryClient.setQueriesData({ queryKey: ['payments'] }, (old: any) => {
    if (!old?.data || !Array.isArray(old.data)) return old
    return {
      ...old,
      data: old.data.map((p: any) => (p.id === payment.id ? { ...p, ...payment } : p)),
    }
  })
}

export function usePayments(status?: string) {
  return useQuery({
    queryKey: ['payments', status],
    queryFn: () => paymentsApi.list({ status }),
    refetchOnMount: 'always',
  })
}

export function usePayment(id: string) {
  return useQuery({
    queryKey: ['payment', id],
    queryFn: () => paymentsApi.get(id),
    enabled: !!id,
    refetchInterval: (query) => {
      const status = (query.state.data as any)?.status
      // Poll short-lived transitional states so webhook/async updates reflect quickly.
      if (status === 'pending' || status === 'authorized' || status === 'partially_refunded') {
        return 5000
      }
      return false
    },
  })
}

export function useCreatePayment() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: CreatePaymentRequest) => paymentsApi.create(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
  })
}

export function useCapturePayment() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ id, amount }: { id: string; amount?: number }) =>
      paymentsApi.capture(id, amount),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['payment', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
  })
}

export function useRefundPayment() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: RefundRequest }) =>
      paymentsApi.refund(id, request),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['payment', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
  })
}

export function useCompletePaymentAuthentication() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id }: { id: string }) => paymentsApi.completeAuthentication(id),
    onSuccess: (payment, variables) => {
      queryClient.setQueryData(['payment', variables.id], payment)
      updatePaymentInListCaches(queryClient, payment)
      queryClient.invalidateQueries({ queryKey: ['payment', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
  })
}
