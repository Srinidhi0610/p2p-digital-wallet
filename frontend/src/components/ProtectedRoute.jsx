import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { Loader2 } from 'lucide-react'

export function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return (
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '70vh',
          gap: '1rem',
          color: 'var(--text-muted)',
        }}
      >
        <Loader2 size={36} className="spin" color="var(--primary)" />
        <p style={{ fontSize: '0.95rem' }}>Authenticating secure session...</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children
}
