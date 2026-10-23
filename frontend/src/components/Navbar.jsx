import { useState } from 'react'
import { NavLink, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import {
  Wallet,
  Send,
  History,
  User,
  LogOut,
  Menu,
  X,
  ArrowUpRight,
} from 'lucide-react'

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  const closeMenu = () => setMobileMenuOpen(false)

  return (
    <header className="navbar">
      <div className="navbar-inner">
        {/* Brand Container with pocket // peer mark */}
        <Link to="/" className="brand-container" onClick={closeMenu}>
          <div className="brand-mark" title="pocket // peer P2P ledger">
            <span style={{ position: 'relative', zIndex: 1 }}>
              <ArrowUpRight size={16} color="var(--navy)" strokeWidth={3} />
            </span>
          </div>
          <div className="brand-text">
            <div className="brand-title">
              pocket <span className="slash">//</span> peer
            </div>
            <div className="brand-subtitle">MONEY IN MOTION</div>
          </div>
        </Link>

        {/* Mobile menu toggle */}
        {isAuthenticated && (
          <button
            className="mobile-nav-toggle"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle Navigation"
          >
            {mobileMenuOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        )}

        {/* Navigation Links */}
        {isAuthenticated ? (
          <nav className={`nav-links ${mobileMenuOpen ? 'mobile-open' : ''}`}>
            <NavLink
              to="/"
              end
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              onClick={closeMenu}
            >
              <Wallet size={16} />
              <span>Overview</span>
            </NavLink>

            <NavLink
              to="/send"
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              onClick={closeMenu}
            >
              <Send size={16} />
              <span>Send Money</span>
            </NavLink>

            <NavLink
              to="/transactions"
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              onClick={closeMenu}
            >
              <History size={16} />
              <span>Ledger</span>
            </NavLink>

            <NavLink
              to="/profile"
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              onClick={closeMenu}
            >
              <User size={16} />
              <span>Accounts</span>
            </NavLink>

            {/* User Profile Chip & Logout */}
            <div className="nav-user-chip" style={{ marginLeft: '0.5rem' }}>
              <div className="user-avatar-mark">
                {user?.name ? user.name.charAt(0).toUpperCase() : 'P'}
              </div>
              <div className="user-meta-text">
                <span className="user-display-name">{user?.name || 'Account'}</span>
                <span className="user-id-indicator">#UID-{user?.id}</span>
              </div>
              <button
                onClick={handleLogout}
                className="btn btn-ghost btn-sm"
                title="Sign out of wallet"
                style={{ padding: '0.25rem 0.4rem', color: 'var(--muted)' }}
              >
                <LogOut size={15} />
              </button>
            </div>
          </nav>
        ) : (
          <div className="nav-links">
            <Link to="/login" className="btn btn-ghost btn-sm">
              Sign In
            </Link>
            <Link to="/register" className="btn btn-primary btn-sm">
              Create Account
            </Link>
          </div>
        )}
      </div>
    </header>
  )
}
