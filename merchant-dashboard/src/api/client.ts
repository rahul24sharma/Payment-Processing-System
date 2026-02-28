import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const PUBLIC_AUTH_PATHS = new Set([
  '/auth/login',
  '/auth/register',
  '/auth/forgot-password',
  '/auth/reset-password',
  '/auth/health',
])

function isPublicAuthRequest(url?: string, baseURL?: string): boolean {
  const rawUrl = url || ''
  const normalizedBase = baseURL || ''
  const combined = `${normalizedBase}${rawUrl}`

  if (PUBLIC_AUTH_PATHS.has(rawUrl)) {
    return true
  }

  return (
    combined.includes('/api/v1/auth/login') ||
    combined.includes('/api/v1/auth/register') ||
    combined.includes('/api/v1/auth/forgot-password') ||
    combined.includes('/api/v1/auth/reset-password') ||
    combined.includes('/api/v1/auth/health')
  )
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds
})

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    const requestIsPublicAuth = isPublicAuthRequest(config.url, config.baseURL ?? API_BASE_URL)
    const token = localStorage.getItem('api_token')
    const merchantId = localStorage.getItem('merchant_id')

    if (!requestIsPublicAuth && token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    if (!requestIsPublicAuth && merchantId) {
      config.headers['X-Merchant-Id'] = merchantId
    }

    if (requestIsPublicAuth) {
      delete config.headers.Authorization
      delete config.headers['X-Merchant-Id']
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message)
    
    if (error.response?.status === 401) {
      localStorage.removeItem('api_token')
      window.location.href = '/login'
    }
    
    return Promise.reject(error)
  }
)
