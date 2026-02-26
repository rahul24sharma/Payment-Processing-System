import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { settingsApi } from '@/api/settings'
import type {
  UpdateProfileRequest,
  ChangePasswordRequest,
  UpdateBankAccountRequest,
  UpdateNotificationPreferencesRequest,
} from '@/types/settings'

export function useMerchantProfile() {
  return useQuery({
    queryKey: ['merchant-profile'],
    queryFn: () => settingsApi.getProfile(),
  })
}

export function useUpdateProfile() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: UpdateProfileRequest) => settingsApi.updateProfile(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['merchant-profile'] })
    },
  })
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (request: ChangePasswordRequest) => settingsApi.changePassword(request),
  })
}

export function useUpdateBankAccount() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: UpdateBankAccountRequest) => settingsApi.updateBankAccount(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['merchant-profile'] })
    },
  })
}

export function useUpdateNotificationPreferences() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (request: UpdateNotificationPreferencesRequest) =>
      settingsApi.updateNotificationPreferences(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['merchant-profile'] })
    },
  })
}

export function useDeleteAccount() {
  return useMutation({
    mutationFn: () => settingsApi.deleteAccount(),
  })
}