import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { paymentsApi } from '@/api/payments'
import type { CreatePaymentRequest, RefundRequest } from '@/types/payment'

export function usePayments(status?: string) {
  return useQuery({
    queryKey: ['payments', status],
    queryFn: () => paymentsApi.list({ status }),
  })
}

export function usePayment(id: string) {
  return useQuery({
    queryKey: ['payment', id],
    queryFn: () => paymentsApi.get(id),
    enabled: !!id,
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