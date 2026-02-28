import { apiClient } from './client'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  businessName: string
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  merchantId: string
  businessName: string
  email: string
  role: string
  apiKey?: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ForgotPasswordResponse {
  message: string
  resetToken?: string | null
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

export interface ResetPasswordResponse {
  message: string
}

export const authApi = {
  register: async (request: RegisterRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/auth/register', request)
    return response.data
  },

  login: async (request: LoginRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/auth/login', request)
    return response.data
  },

  forgotPassword: async (request: ForgotPasswordRequest): Promise<ForgotPasswordResponse> => {
    const response = await apiClient.post<ForgotPasswordResponse>('/auth/forgot-password', request)
    return response.data
  },

  resetPassword: async (request: ResetPasswordRequest): Promise<ResetPasswordResponse> => {
    const response = await apiClient.post<ResetPasswordResponse>('/auth/reset-password', request)
    return response.data
  },

  logout: () => {
    localStorage.removeItem('api_token')
    localStorage.removeItem('merchant_id')
    localStorage.removeItem('merchant_email')
    localStorage.removeItem('merchant_role')
  },

  isAuthenticated: (): boolean => {
    return !!localStorage.getItem('api_token')
  },

  getToken: (): string | null => {
    return localStorage.getItem('api_token')
  },
}
