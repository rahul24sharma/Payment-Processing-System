import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { webhooksApi } from '@/api/webhooks'
import type { CreateWebhookEndpointRequest } from '@/types/webhook'

export function useWebhookEndpoints() {
  return useQuery({
    queryKey: ['webhook-endpoints'],
    queryFn: () => webhooksApi.listEndpoints(),
  })
}

export function useWebhookLogs() {
  return useQuery({
    queryKey: ['webhook-logs'],
    queryFn: () => webhooksApi.getLogs(),
    refetchInterval: 10000, // Refresh every 10 seconds
  })
}

export function useCreateWebhookEndpoint() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: CreateWebhookEndpointRequest) => 
      webhooksApi.createEndpoint(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhook-endpoints'] })
    },
  })
}

export function useDeleteWebhookEndpoint() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => webhooksApi.deleteEndpoint(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhook-endpoints'] })
      queryClient.invalidateQueries({ queryKey: ['webhook-logs'] })
    },
  })
}

export function useTestWebhook() {
  return useMutation({
    mutationFn: (id: string) => webhooksApi.testEndpoint(id),
  })
}