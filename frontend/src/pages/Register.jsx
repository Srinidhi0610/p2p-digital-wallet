import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { User, Mail, Lock, ArrowRight, AlertCircle, Eye, EyeOff, CheckCircle2, ShieldCheck } from 'lucide-react'

export function Register() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const { register } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)

    if (!name.trim()) {
      setError('Please provide your full legal name')
      return
    }
    if (!email.trim() || !email.includes('@')) {
      setError('Please provide a valid email address')
      return
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters long')
      return
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match')
      return
    }

    setIsSubmitting(true)
    try {
      await register(name.trim(), email.trim().toLowerCase(), password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.message || 'Registration failed. The email address may already exist.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const isPasswordValid = password.length >= 8
  const doPasswordsMatch = password && password === confirmPassword

  return (
    <div className="auth-split-wrapper">
      {/* Brand & Editorial Showcase (Left Side) */}
      <div className="auth-brand-side">
        <div>
          <div className="auth-brand-badge">
            <ShieldCheck size={13} />
            <span>JOIN THE PEER NETWORK</span>
          </div>

          <h1 className="auth-brand-heading">
            Decentralized <br />
            <span style={{ color: 'var(--lime)' }}>Precision.</span>
          </h1>

          <p className="auth-brand-desc">
            Activate your digital wallet account in seconds. Open multi-currency accounts and experience guaranteed double-entry settlement.
          </p>
        </div>

        {/* Abstract vector wave */}
        <div style={{ margin: '2rem 0' }}>
          <svg width="100%" height="70" viewBox="0 0 400 70" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M0 20 Q 100 60, 200 20 T 400 20"
              stroke="var(--lime)"
              strokeWidth="2"
              opacity="0.8"
            />
            <path
              d="M0 45 Q 100 10, 200 45 T 400 45"
              stroke="var(--coral)"
              strokeWidth="1.5"
              opacity="0.5"
              strokeDasharray="4 4"
            />
          </svg>
        </div>

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
              ACCOUNT TYPE
            </div>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--ivory)', marginTop: '0.2rem' }}>
              Peer Settlement Node
            </div>
          </div>
          <div>
            <div style={{ fontSize: '0.68rem', fontFamily: 'var(--font-mono)', color: 'var(--muted)', textTransform: 'uppercase' }}>
              INITIAL BALANCE
            </div>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--lime)', marginTop: '0.2rem' }}>
              0.0000 (Strict Audit)
            </div>
          </div>
        </div>
      </div>

      {/* Form Area (Right Side) */}
      <div className="auth-form-side">
        <div style={{ marginBottom: '1.75rem' }}>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--ivory)' }}>
            Create Account
          </h2>
          <p style={{ color: 'var(--muted)', fontSize: '0.88rem', marginTop: '0.35rem' }}>
            Set up your identity and credentials
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
            <label className="form-label" htmlFor="register-name">
              Full Legal Name
            </label>
            <div className="form-input-container">
              <User size={16} className="form-input-icon" />
              <input
                id="register-name"
                type="text"
                className="form-input form-input-with-icon"
                placeholder="Alice Smith"
                value={name}
                onChange={(e) => setName(e.target.value)}
                maxLength={100}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="register-email">
              Email Address
            </label>
            <div className="form-input-container">
              <Mail size={16} className="form-input-icon" />
              <input
                id="register-email"
                type="email"
                className="form-input form-input-with-icon"
                placeholder="alice@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                maxLength={150}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="register-password">
              Password
              <span className="form-label-hint">Min 8 chars</span>
            </label>
            <div className="form-input-container">
              <Lock size={16} className="form-input-icon" />
              <input
                id="register-password"
                type={showPassword ? 'text' : 'password'}
                className="form-input form-input-with-icon"
                placeholder="Create password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                maxLength={100}
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

          <div className="form-group">
            <label className="form-label" htmlFor="register-confirm-password">
              Confirm Password
            </label>
            <div className="form-input-container">
              <Lock size={16} className="form-input-icon" />
              <input
                id="register-confirm-password"
                type={showPassword ? 'text' : 'password'}
                className="form-input form-input-with-icon"
                placeholder="Repeat password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </div>
            {confirmPassword && (
              <div
                style={{
                  fontSize: '0.75rem',
                  marginTop: '0.25rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  color: doPasswordsMatch ? 'var(--lime)' : 'var(--coral)',
                }}
              >
                {doPasswordsMatch ? (
                  <>
                    <CheckCircle2 size={13} /> Passwords match
                  </>
                ) : (
                  <>
                    <AlertCircle size={13} /> Passwords do not match
                  </>
                )}
              </div>
            )}
          </div>

          <button
            type="submit"
            className="btn btn-lime btn-full btn-lg"
            style={{ marginTop: '1.25rem' }}
            disabled={isSubmitting || !isPasswordValid || !doPasswordsMatch}
          >
            <span>{isSubmitting ? 'Creating Account...' : 'Activate Wallet Node'}</span>
            <ArrowRight size={16} />
          </button>
        </form>

        <div
          style={{
            marginTop: '1.75rem',
            paddingTop: '1rem',
            borderTop: '1px solid var(--slate-border)',
            textAlign: 'center',
            fontSize: '0.85rem',
            color: 'var(--muted)',
          }}
        >
          Already activated?{' '}
          <Link to="/login" style={{ fontWeight: 700, color: 'var(--lime)' }}>
            Sign in
          </Link>
        </div>
      </div>
    </div>
  )
}
