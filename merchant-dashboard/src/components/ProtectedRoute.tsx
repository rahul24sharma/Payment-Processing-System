import { Navigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import AsyncState from '@/components/ui/AsyncState'

interface Props {
  children: React.ReactNode
  requiredRoles?: string[]
}

export default function ProtectedRoute({ children, requiredRoles }: Props) {
  const { isAuthenticated, role, loading } = useAuth()
  
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

  if (requiredRoles && requiredRoles.length > 0) {
    const currentRole = role?.toUpperCase()
    if (!currentRole || !requiredRoles.includes(currentRole)) {
      return (
        <AsyncState
          kind="error"
          title="Access denied"
          message="Your role does not have permission to view this page."
        />
      )
    }
  }
  
  return <>{children}</>
}
