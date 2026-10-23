import { apiRequest, setStoredToken, setStoredUser, clearAuthStorage } from './api'

export const authService = {
  /**
   * Register a new user account.
   * On success, backend returns AuthResponse { token, tokenType, expiresInSeconds, user }
   */
  async register({ name, email, password }) {
    const response = await apiRequest('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password }),
    })
    if (response?.token) {
      setStoredToken(response.token)
      setStoredUser(response.user)
    }
    return response
  },

  /**
   * Login with email and password.
   * On success, backend returns AuthResponse { token, tokenType, expiresInSeconds, user }
   */
  async login({ email, password }) {
    const response = await apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
    if (response?.token) {
      setStoredToken(response.token)
      setStoredUser(response.user)
    }
    return response
  },

  /**
   * Fetch current authenticated user's profile.
   * Backend returns UserResponse { id, name, email, createdAt }
   */
  async getCurrentUser() {
    return await apiRequest('/auth/me', {
      method: 'GET',
    })
  },

  /**
   * Check backend service health status.
   * Public endpoint /api/health
   */
  async checkHealth() {
    return await apiRequest('/health', {
      method: 'GET',
    })
  },

  /**
   * Logout user by removing stored tokens.
   */
  logout() {
    clearAuthStorage()
  },
}
