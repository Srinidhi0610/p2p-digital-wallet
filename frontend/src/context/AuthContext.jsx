import { useState, useEffect, useCallback } from 'react'
import { AuthContext } from './authContextInstance'
import { authService } from '../services/authService'
import { getStoredToken, getStoredUser, clearAuthStorage } from '../services/api'

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => getStoredToken())
  const [user, setUser] = useState(() => getStoredUser())
  const [isLoading, setIsLoading] = useState(true)

  // Verify stored session on app load
  useEffect(() => {
    let isMounted = true

    async function initAuth() {
      const storedToken = getStoredToken()
      if (storedToken) {
        try {
          const profile = await authService.getCurrentUser()
          if (isMounted) {
            setUser(profile)
          }
        } catch {
          // Token is invalid/expired
          if (isMounted) {
            clearAuthStorage()
            setToken(null)
            setUser(null)
          }
        }
      }
      if (isMounted) {
        setIsLoading(false)
      }
    }

    initAuth()

    // Listen for unauthorized 401 events dispatched by apiRequest
    function handleUnauthorized() {
      setToken(null)
      setUser(null)
    }

    window.addEventListener('auth:unauthorized', handleUnauthorized)

    return () => {
      isMounted = false
      window.removeEventListener('auth:unauthorized', handleUnauthorized)
    }
  }, [])

  const login = useCallback(async (email, password) => {
    const response = await authService.login({ email, password })
    setToken(response.token)
    setUser(response.user)
    return response
  }, [])

  const register = useCallback(async (name, email, password) => {
    const response = await authService.register({ name, email, password })
    setToken(response.token)
    setUser(response.user)
    return response
  }, [])

  const logout = useCallback(() => {
    authService.logout()
    setToken(null)
    setUser(null)
  }, [])

  const refreshProfile = useCallback(async () => {
    try {
      const updatedProfile = await authService.getCurrentUser()
      setUser(updatedProfile)
      return updatedProfile
    } catch {
      return null
    }
  }, [])

  const value = {
    user,
    token,
    isAuthenticated: !!token && !!user,
    isLoading,
    login,
    register,
    logout,
    refreshProfile,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
