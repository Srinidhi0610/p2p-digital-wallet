/**
 * Base API Client for interacting with the Spring Boot REST backend.
 * Uses native fetch with standard request/response interceptors.
 */

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const TOKEN_KEY = 'apex_wallet_token'
const USER_KEY = 'apex_wallet_user'

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export function getStoredUser() {
  const userJson = localStorage.getItem(USER_KEY)
  try {
    return userJson ? JSON.parse(userJson) : null
  } catch {
    return null
  }
}

export function setStoredUser(user) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

export function clearAuthStorage() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/**
 * Core HTTP request handler
 */
export async function apiRequest(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`

  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...options.headers,
  }

  const token = getStoredToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const config = {
    ...options,
    headers,
  }

  let response
  try {
    response = await fetch(url, config)
  } catch (err) {
    throw new ApiError(
      'Unable to connect to wallet server. Please ensure the backend is running on port 8080.',
      0,
      { originalError: err.message }
    )
  }

  // Parse JSON response body if present
  let data = null
  const contentType = response.headers.get('content-type')
  if (contentType && contentType.includes('application/json')) {
    try {
      data = await response.json()
    } catch {
      data = null
    }
  } else {
    try {
      const text = await response.text()
      if (text) data = { message: text }
    } catch {
      data = null
    }
  }

  // Handle Unauthorized (401)
  if (response.status === 401) {
    clearAuthStorage()
    // Emit custom event so AuthContext can react without hard window reload
    window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    const errorMsg = data?.message || 'Session expired or invalid credentials. Please log in again.'
    throw new ApiError(errorMsg, 401, data)
  }

  if (!response.ok) {
    // Backend standard format: { timestamp, status, error, message }
    const message = data?.message || data?.error || `Request failed with status ${response.status}`
    throw new ApiError(message, response.status, data)
  }

  return data
}
