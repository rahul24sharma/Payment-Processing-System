import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiKeysApi } from '@/api/apiKeys'
import type { CreateApiKeyRequest } from '@/types/apiKey'

export function useApiKeys() {
  return useQuery({
    queryKey: ['api-keys'],
    queryFn: () => apiKeysApi.list(),
  })
}

export function useCreateApiKey() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: CreateApiKeyRequest) => apiKeysApi.create(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] })
    },
  })
}

export function useRevokeApiKey() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => apiKeysApi.revoke(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] })
    },
  })
}