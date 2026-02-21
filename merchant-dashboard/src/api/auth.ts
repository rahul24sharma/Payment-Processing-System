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
  apiKey?: string
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

  logout: () => {
    localStorage.removeItem('api_token')
    localStorage.removeItem('merchant_id')
    localStorage.removeItem('merchant_email')
  },

  isAuthenticated: (): boolean => {
    return !!localStorage.getItem('api_token')
  },

  getToken: (): string | null => {
    return localStorage.getItem('api_token')
  },
}