import { apiClient } from './client'
import type {
  MerchantSettings,
  UpdateProfileRequest,
  ChangePasswordRequest,
  UpdateBankAccountRequest,
  UpdateNotificationPreferencesRequest,
} from '@/types/settings'

export const settingsApi = {
  /**
   * Get merchant profile
   */
  getProfile: async (): Promise<MerchantSettings> => {
    const response = await apiClient.get<MerchantSettings>('/merchants/me')
    return response.data
  },

  /**
   * Update profile
   */
  updateProfile: async (request: UpdateProfileRequest): Promise<MerchantSettings> => {
    const response = await apiClient.put<MerchantSettings>('/merchants/me', request)
    return response.data
  },

  /**
   * Change password
   */
  changePassword: async (request: ChangePasswordRequest): Promise<void> => {
    await apiClient.post('/merchants/me/password', request)
  },

  /**
   * Update bank account
   */
  updateBankAccount: async (request: UpdateBankAccountRequest): Promise<void> => {
    await apiClient.put('/merchants/me/bank-account', request)
  },

  /**
   * Update notification preferences
   */
  updateNotificationPreferences: async (
    request: UpdateNotificationPreferencesRequest
  ): Promise<void> => {
    await apiClient.put('/merchants/me/notifications', request)
  },

  /**
   * Delete account
   */
  deleteAccount: async (): Promise<void> => {
    await apiClient.delete('/merchants/me')
  },
}