import { createContext, useContext, useState, useEffect } from 'react'
import { authApi, type AuthResponse } from '@/api/auth'

interface AuthContextType {
  isAuthenticated: boolean
  merchantId: string | null
  email: string | null
  role: string | null
  login: (email: string, password: string) => Promise<void>
  register: (businessName: string, email: string, password: string) => Promise<AuthResponse>
  logout: () => void
  loading: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [merchantId, setMerchantId] = useState<string | null>(null)
  const [email, setEmail] = useState<string | null>(null)
  const [role, setRole] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  
  useEffect(() => {
    // Check if user is already logged in
    const token = localStorage.getItem('api_token')
    const storedMerchantId = localStorage.getItem('merchant_id')
    const storedEmail = localStorage.getItem('merchant_email')
    const storedRole = localStorage.getItem('merchant_role')
    
    if (token && storedMerchantId && storedEmail) {
      setIsAuthenticated(true)
      setMerchantId(storedMerchantId)
      setEmail(storedEmail)
      setRole(storedRole)
    }
    
    setLoading(false)
  }, [])
  
  const login = async (email: string, password: string) => {
    const response = await authApi.login({ email, password })
    
    localStorage.setItem('api_token', response.token)
    localStorage.setItem('merchant_id', response.merchantId)
    localStorage.setItem('merchant_email', response.email)
    localStorage.setItem('merchant_role', response.role)
    
    setIsAuthenticated(true)
    setMerchantId(response.merchantId)
    setEmail(response.email)
    setRole(response.role)
  }
  
  const register = async (businessName: string, email: string, password: string) => {
    const response = await authApi.register({ businessName, email, password })
    
    localStorage.setItem('api_token', response.token)
    localStorage.setItem('merchant_id', response.merchantId)
    localStorage.setItem('merchant_email', response.email)
    localStorage.setItem('merchant_role', response.role)
    
    setIsAuthenticated(true)
    setMerchantId(response.merchantId)
    setEmail(response.email)
    setRole(response.role)
    
    return response
  }
  
  const logout = () => {
    authApi.logout()
    setIsAuthenticated(false)
    setMerchantId(null)
    setEmail(null)
    setRole(null)
  }
  
  return (
    <AuthContext.Provider value={{ isAuthenticated, merchantId, email, role, login, register, logout, loading }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
