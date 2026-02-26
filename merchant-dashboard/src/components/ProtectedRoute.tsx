import { Navigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import AsyncState from '@/components/ui/AsyncState'

interface Props {
  children: React.ReactNode
}

export default function ProtectedRoute({ children }: Props) {
  const { isAuthenticated, loading } = useAuth()
  
  if (loading) {
    return (
      <AsyncState
        kind="loading"
        title="Checking your session"
        message="Please wait while we verify your account access."
      />
    )
  }
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  
  return <>{children}</>
}
