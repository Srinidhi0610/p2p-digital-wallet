import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../context/useAuth'
import { walletService } from '../services/walletService'
import { formatCurrency, formatDateTime } from '../utils/formatters'
import {
  Mail,
  Calendar,
  Wallet,
  Plus,
  AlertCircle,
  CheckCircle2,
  Copy,
  Check,
  RefreshCw,
  Hash,
  Zap,
} from 'lucide-react'

const COMMON_CURRENCIES = ['USD', 'EUR', 'GBP', 'CAD', 'AUD', 'INR', 'JPY', 'SGD', 'CHF']

export function Profile() {
  const { user, refreshProfile } = useAuth()
  const [wallets, setWallets] = useState([])
  const [newCurrency, setNewCurrency] = useState('USD')
  const [customCurrency, setCustomCurrency] = useState('')
  const [useCustom, setUseCustom] = useState(false)

  const [isLoadingWallets, setIsLoadingWallets] = useState(true)
  const [isCreating, setIsCreating] = useState(false)
  const [error, setError] = useState(null)
  const [successMsg, setSuccessMsg] = useState(null)
  const [copiedWalletId, setCopiedWalletId] = useState(null)

  const loadWallets = useCallback(async () => {
    setIsLoadingWallets(true)
    try {
      const data = await walletService.getWallets()
      setWallets(data || [])
    } catch (err) {
      setError(err.message || 'Failed to synchronize wallet accounts.')
    } finally {
      setIsLoadingWallets(false)
    }
  }, [])

  useEffect(() => {
    loadWallets()
    refreshProfile()
  }, [loadWallets, refreshProfile])

  async function handleCreateWallet(e) {
    e.preventDefault()
    setError(null)
    setSuccessMsg(null)

    const targetCurrency = useCustom
      ? customCurrency.trim().toUpperCase()
      : newCurrency.trim().toUpperCase()

    if (!targetCurrency || targetCurrency.length !== 3) {
      setError('Currency code must be exactly 3 alphabetic letters (e.g. USD, EUR, GBP)')
      return
    }

    if (wallets.some((w) => w.currency === targetCurrency)) {
      setError(`An active ${targetCurrency} wallet node is already allocated to your account.`)
      return
    }

    setIsCreating(true)
    try {
      const created = await walletService.createWallet(targetCurrency)
      setSuccessMsg(`Activated new ${created.currency} Wallet Partition (ID #${created.id})`)
      setCustomCurrency('')
      await loadWallets()
    } catch (err) {
      setError(err.message || 'Failed to create wallet account')
    } finally {
      setIsCreating(false)
    }
  }

  function handleCopy(walletId) {
    navigator.clipboard.writeText(String(walletId))
    setCopiedWalletId(walletId)
    setTimeout(() => setCopiedWalletId(null), 2000)
  }

  return (
    <div style={{ maxWidth: '1040px', margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: '2rem' }}>
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.45rem',
            fontFamily: 'var(--font-mono)',
            fontSize: '0.72rem',
            color: 'var(--lime)',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            marginBottom: '0.35rem',
          }}
        >
          <Zap size={13} />
          <span>IDENTITY & PARTITIONS</span>
        </div>
        <h1 style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--ivory)' }}>
          Accounts & Settings
        </h1>
        <p style={{ color: 'var(--muted)', fontSize: '0.88rem' }}>
          Peer credentials and multi-currency ledger accounts
        </p>
      </div>

      {error && (
        <div className="alert alert-danger">
          <AlertCircle size={18} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      {successMsg && (
        <div className="alert alert-success">
          <CheckCircle2 size={18} style={{ flexShrink: 0 }} />
          <span>{successMsg}</span>
        </div>
      )}

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: '1.5rem',
          marginBottom: '2rem',
        }}
      >
        {/* User Identity Card */}
        <div className="surface">
          <div className="surface-header">
            <h2 className="surface-title">Peer Identity</h2>
            <span className="status-pill settled">VERIFIED PEER</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '1rem',
                padding: '1rem',
                background: 'var(--navy)',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--slate-border)',
              }}
            >
              <div
                style={{
                  width: '44px',
                  height: '44px',
                  borderRadius: 'var(--radius-xs)',
                  background: 'var(--lime)',
                  color: 'var(--lime-text)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 800,
                  fontSize: '1.2rem',
                }}
              >
                {user?.name ? user.name.charAt(0).toUpperCase() : 'P'}
              </div>
              <div>
                <div style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--ivory)' }}>
                  {user?.name || 'Account'}
                </div>
                <div style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--lime)' }}>
                  PEER NODE #{user?.id}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.88rem' }}>
              <Mail size={16} color="var(--muted)" />
              <span style={{ color: 'var(--muted)' }}>Email:</span>
              <span className="font-mono" style={{ color: 'var(--ivory)', fontWeight: 600 }}>
                {user?.email}
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.88rem' }}>
              <Hash size={16} color="var(--muted)" />
              <span style={{ color: 'var(--muted)' }}>System User ID:</span>
              <span className="font-mono" style={{ color: 'var(--ivory)' }}>
                #{user?.id}
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.88rem' }}>
              <Calendar size={16} color="var(--muted)" />
              <span style={{ color: 'var(--muted)' }}>Joined:</span>
              <span style={{ color: 'var(--ivory)', fontSize: '0.82rem' }}>
                {formatDateTime(user?.createdAt)}
              </span>
            </div>
          </div>
        </div>

        {/* Create Currency Wallet Surface */}
        <div className="surface">
          <div className="surface-header">
            <div>
              <h2 className="surface-title">Open Currency Account</h2>
              <div className="surface-subtitle">Allocate an isolated ledger balance partition</div>
            </div>
            <Wallet size={18} color="var(--lime)" />
          </div>

          <form onSubmit={handleCreateWallet}>
            <div className="form-group">
              <label className="form-label" htmlFor="currency-select">
                Currency Code (ISO-4217)
              </label>
              {!useCustom ? (
                <select
                  id="currency-select"
                  className="form-input font-mono"
                  value={newCurrency}
                  onChange={(e) => setNewCurrency(e.target.value)}
                >
                  {COMMON_CURRENCIES.map((curr) => {
                    const alreadyExists = wallets.some((w) => w.currency === curr)
                    return (
                      <option key={curr} value={curr} disabled={alreadyExists}>
                        {curr} {alreadyExists ? '(Already Allocated)' : ''}
                      </option>
                    )
                  })}
                </select>
              ) : (
                <input
                  type="text"
                  className="form-input font-mono"
                  placeholder="e.g. CHF, JPY"
                  maxLength={3}
                  value={customCurrency}
                  onChange={(e) => setCustomCurrency(e.target.value.toUpperCase())}
                  required
                />
              )}
            </div>

            <div style={{ marginBottom: '1.25rem' }}>
              <button
                type="button"
                onClick={() => setUseCustom(!useCustom)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--lime)',
                  fontSize: '0.78rem',
                  cursor: 'pointer',
                  textDecoration: 'underline',
                  fontFamily: 'var(--font-mono)',
                }}
              >
                {useCustom ? '← Pick from common ISO currencies' : '→ Enter custom 3-character code'}
              </button>
            </div>

            <button
              type="submit"
              className="btn btn-lime btn-full"
              disabled={isCreating}
            >
              <Plus size={15} />
              <span>{isCreating ? 'Allocating...' : 'Initialize Wallet Partition'}</span>
            </button>
          </form>
        </div>
      </div>

      {/* Existing Wallets Table */}
      <div className="surface" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: '1.5rem 1.5rem 1rem' }}>
          <div className="surface-header" style={{ marginBottom: 0 }}>
            <div>
              <h2 className="surface-title">Allocated Wallet Partitions ({wallets.length})</h2>
              <div className="surface-subtitle">
                Share your numeric Wallet ID with counterparties to receive peer transfers
              </div>
            </div>
            <button
              onClick={loadWallets}
              className="btn btn-ghost btn-sm"
              disabled={isLoadingWallets}
            >
              <RefreshCw size={13} className={isLoadingWallets ? 'spin' : ''} />
            </button>
          </div>
        </div>

        {isLoadingWallets ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--muted)' }}>
            <RefreshCw size={24} className="spin" style={{ margin: '0 auto 0.5rem' }} />
            <div>Synchronizing partitions...</div>
          </div>
        ) : wallets.length === 0 ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--muted)' }}>
            No wallet accounts active. Use the allocation form above to open your primary account.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="editorial-table">
              <thead>
                <tr>
                  <th>Partition ID</th>
                  <th>Currency</th>
                  <th>Authoritative Balance</th>
                  <th>Allocated Date</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {wallets.map((wallet) => (
                  <tr key={wallet.id}>
                    <td>
                      <span className="font-mono" style={{ fontWeight: 700, color: 'var(--ivory)' }}>
                        WALLET #{wallet.id}
                      </span>
                    </td>
                    <td>
                      <span className="currency-pill">
                        {wallet.currency}
                      </span>
                    </td>
                    <td>
                      <span className="font-mono" style={{ fontWeight: 700, color: 'var(--lime)' }}>
                        {formatCurrency(wallet.balance, wallet.currency)}
                      </span>
                    </td>
                    <td style={{ color: 'var(--muted)', fontSize: '0.8rem', fontFamily: 'var(--font-mono)' }}>
                      {formatDateTime(wallet.createdAt)}
                    </td>
                    <td>
                      <button
                        onClick={() => handleCopy(wallet.id)}
                        className="btn btn-secondary btn-sm"
                        title="Copy Wallet ID to clipboard"
                        style={{ fontSize: '0.75rem', padding: '0.25rem 0.55rem' }}
                      >
                        {copiedWalletId === wallet.id ? (
                          <>
                            <Check size={13} color="var(--lime)" />
                            <span style={{ color: 'var(--lime)' }}>Copied</span>
                          </>
                        ) : (
                          <>
                            <Copy size={13} />
                            <span>Copy ID</span>
                          </>
                        )}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
