import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { Lock, Mail, ArrowRight, AlertCircle, Eye, EyeOff, Zap } from 'lucide-react'

export function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from?.pathname || '/'

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)

    if (!email.trim()) {
      setError('Email address is required')
      return
    }
    if (!password) {
      setError('Password cannot be blank')
      return
    }

    setIsSubmitting(true)
    try {
      await login(email.trim(), password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err.message || 'Authentication failed. Please verify credentials.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="auth-split-wrapper">
      {/* Brand & Editorial Showcase (Left Side) */}
      <div className="auth-brand-side">
        <div>
          <div className="auth-brand-badge">
            <Zap size={13} />
            <span>PEER-TO-PEER LEDGER ENGINE</span>
          </div>

          <h1 className="auth-brand-heading">
            Money in <br />
            <span style={{ color: 'var(--lime)' }}>Motion.</span>
          </h1>

          <p className="auth-brand-desc">
            A real-time digital wallet built on double-entry accounting principles, deterministic row-level locks, and cryptographic idempotency.
          </p>
        </div>

        {/* Abstract Vector Flow Graphics */}
        <div style={{ margin: '2rem 0' }}>
          <svg width="100%" height="80" viewBox="0 0 400 80" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M0 40 C 100 10, 200 70, 300 30 C 350 10, 380 40, 400 40"
              stroke="var(--slate-border-strong)"
              strokeWidth="2"
              strokeDasharray="4 4"
            />
            <path
              d="M0 50 C 80 20, 160 80, 260 40 C 320 20, 370 60, 400 50"
              stroke="var(--lime)"
              strokeWidth="2"
              opacity="0.6"
            />
            <circle cx="260" cy="40" r="4" fill="var(--lime)" />
            <circle cx="100" cy="27" r="3" fill="var(--coral)" />
          </svg>
        </div>

        {/* Feature Specs */}
        <div
          style={{
            borderTop: '1px solid var(--slate-border)',
            paddingTop: '1.5rem',
            display: 'grid',
            gridTemplateColumns: 'repeat(2, 1fr)',
            gap: '1rem',
          }}
        >
          <div>
            <div style={{ fontSize: '0.68rem', fontFamily: 'var(--font-mono)', color: 'var(--muted)', textTransform: 'uppercase' }}>
              SETTLEMENT MODEL
            </div>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--ivory)', marginTop: '0.2rem' }}>
              Double-Entry Ledger
            </div>
          </div>
          <div>
            <div style={{ fontSize: '0.68rem', fontFamily: 'var(--font-mono)', color: 'var(--muted)', textTransform: 'uppercase' }}>
              CONCURRENCY
            </div>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--lime)', marginTop: '0.2rem' }}>
              Pessimistic Row Locks
            </div>
          </div>
        </div>
      </div>

      {/* Form Area (Right Side) */}
      <div className="auth-form-side">
        <div style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--ivory)' }}>
            Access Account
          </h2>
          <p style={{ color: 'var(--muted)', fontSize: '0.88rem', marginTop: '0.35rem' }}>
            Enter your credentials to manage balances and transfer funds
          </p>
        </div>

        {error && (
          <div className="alert alert-danger">
            <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="login-email">
              Email Address
            </label>
            <div className="form-input-container">
              <Mail size={16} className="form-input-icon" />
              <input
                id="login-email"
                type="email"
                className="form-input form-input-with-icon"
                placeholder="alice@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="login-password">
              Password
            </label>
            <div className="form-input-container">
              <Lock size={16} className="form-input-icon" />
              <input
                id="login-password"
                type={showPassword ? 'text' : 'password'}
                className="form-input form-input-with-icon"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
                style={{ paddingRight: '2.5rem' }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  position: 'absolute',
                  right: '0.85rem',
                  background: 'none',
                  border: 'none',
                  color: 'var(--muted)',
                  cursor: 'pointer',
                }}
                tabIndex={-1}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-lime btn-full btn-lg"
            style={{ marginTop: '1.5rem' }}
            disabled={isSubmitting}
          >
            <span>{isSubmitting ? 'Authenticating...' : 'Sign In to Wallet'}</span>
            <ArrowRight size={16} />
          </button>
        </form>

        <div
          style={{
            marginTop: '2rem',
            paddingTop: '1.25rem',
            borderTop: '1px solid var(--slate-border)',
            textAlign: 'center',
            fontSize: '0.85rem',
            color: 'var(--muted)',
          }}
        >
          New peer?{' '}
          <Link to="/register" style={{ fontWeight: 700, color: 'var(--lime)' }}>
            Create an account
          </Link>
        </div>
      </div>
    </div>
  )
}
