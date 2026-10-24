import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { walletService } from '../services/walletService'
import { transferService } from '../services/transferService'
import { authService } from '../services/authService'
import { WalletCard } from '../components/WalletCard'
import { DemoDepositModal } from '../components/DemoDepositModal'
import { formatCurrency, formatDateTime } from '../utils/formatters'
import {
  Send,
  History,
  PlusCircle,
  ArrowUpRight,
  ArrowDownLeft,
  AlertCircle,
  RefreshCw,
  Plus,
  ShieldCheck,
  Zap,
  Coins,
} from 'lucide-react'

export function Dashboard() {
  const { user } = useAuth()
  const [wallets, setWallets] = useState([])
  const [selectedCurrency, setSelectedCurrency] = useState('USD')
  const [recentTransfers, setRecentTransfers] = useState([])
  const [healthStatus, setHealthStatus] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [isCreatingPrimary, setIsCreatingPrimary] = useState(false)
  const [depositTargetWallet, setDepositTargetWallet] = useState(null)

  const loadDashboardData = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const [walletsData, transfersData, health] = await Promise.all([
        walletService.getWallets(),
        transferService.getTransfers({ page: 0, size: 6 }).catch(() => ({ content: [] })),
        authService.checkHealth().catch(() => ({ status: 'UNKNOWN' })),
      ])

      const fetchedWallets = walletsData || []
      setWallets(fetchedWallets)
      setRecentTransfers(transfersData?.content || [])
      setHealthStatus(health)

      if (fetchedWallets.length > 0 && !fetchedWallets.some((w) => w.currency === selectedCurrency)) {
        setSelectedCurrency(fetchedWallets[0].currency)
      }
    } catch (err) {
      setError(err.message || 'Unable to synchronize ledger status.')
    } finally {
      setIsLoading(false)
    }
  }, [selectedCurrency])

  useEffect(() => {
    loadDashboardData()
  }, [loadDashboardData])

  async function handleCreatePrimaryWallet(curr = 'USD') {
    setIsCreatingPrimary(true)
    setError(null)
    try {
      await walletService.createWallet(curr)
      await loadDashboardData()
    } catch (err) {
      setError(err.message || `Failed to initialize ${curr} wallet`)
    } finally {
      setIsCreatingPrimary(false)
    }
  }

  // Active wallet for oversized balance display
  const activeWallet = wallets.find((w) => w.currency === selectedCurrency) || wallets[0]

  return (
    <div>
      {/* Top Ledger Status & Node Identity */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '1.75rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.45rem',
              padding: '0.28rem 0.65rem',
              background: 'var(--slate)',
              border: '1px solid var(--slate-border)',
              borderRadius: 'var(--radius-pill)',
              fontSize: '0.72rem',
              fontFamily: 'var(--font-mono)',
              color: 'var(--ivory)',
            }}
          >
            <span
              style={{
                width: '6px',
                height: '6px',
                borderRadius: '50%',
                background: healthStatus?.status === 'UP' ? 'var(--lime)' : 'var(--coral)',
                boxShadow: healthStatus?.status === 'UP' ? '0 0 8px var(--lime)' : 'none',
              }}
            />
            <span>SETTLEMENT NODE: {healthStatus?.status === 'UP' ? 'ONLINE' : 'CONNECTING'}</span>
          </div>

          <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--muted)' }}>
            PEER #{user?.id}
          </span>
        </div>

        <button
          onClick={loadDashboardData}
          className="btn btn-secondary btn-sm"
          disabled={isLoading}
        >
          <RefreshCw size={13} className={isLoading ? 'spin' : ''} />
          <span>Sync State</span>
        </button>
      </div>

      {error && (
        <div className="alert alert-danger">
          <AlertCircle size={18} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      {/* Hero: Editorial Oversized Balance Display with Motion Flow Graphic */}
      <div className="motion-banner">
        {/* Abstract vector liquidity curves */}
        <svg
          className="motion-flow-svg"
          viewBox="0 0 380 200"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <path
            d="M20 180 C 120 40, 220 220, 360 30"
            stroke="var(--lime)"
            strokeWidth="3"
            opacity="0.3"
          />
          <path
            d="M60 190 C 160 80, 260 180, 370 70"
            stroke="var(--coral)"
            strokeWidth="2"
            strokeDasharray="6 6"
            opacity="0.25"
          />
          <circle cx="360" cy="30" r="5" fill="var(--lime)" />
          <circle cx="220" cy="140" r="4" fill="var(--ivory)" opacity="0.4" />
        </svg>

        <div style={{ position: 'relative', zIndex: 2 }}>
          <div className="oversized-balance-label">
            <Zap size={14} color="var(--lime)" />
            <span>AUTHORITATIVE SETTLED LIQUIDITY</span>
          </div>

          {/* Oversized Balance */}
          <div className="oversized-balance-value">
            {activeWallet ? (
              formatCurrency(activeWallet.balance, activeWallet.currency)
            ) : (
              <span style={{ color: 'var(--muted)' }}>$0.00</span>
            )}
          </div>

          {/* Currency Switcher Chips */}
          {wallets.length > 1 && (
            <div style={{ display: 'flex', gap: '0.45rem', marginBottom: '1.75rem', flexWrap: 'wrap' }}>
              {wallets.map((w) => (
                <button
                  key={w.id}
                  onClick={() => setSelectedCurrency(w.currency)}
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    padding: '0.25rem 0.65rem',
                    borderRadius: 'var(--radius-pill)',
                    background: selectedCurrency === w.currency ? 'var(--lime)' : 'rgba(16, 26, 43, 0.7)',
                    color: selectedCurrency === w.currency ? 'var(--lime-text)' : 'var(--muted)',
                    border: '1px solid',
                    borderColor: selectedCurrency === w.currency ? 'var(--lime)' : 'var(--slate-border)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  {w.currency} ({formatCurrency(w.balance, w.currency)})
                </button>
              ))}
            </div>
          )}

          {/* Primary Action Button (Electric Lime) & Secondary Links */}
          <div style={{ display: 'flex', gap: '0.85rem', flexWrap: 'wrap', alignItems: 'center' }}>
            <Link
              to="/send"
              className="btn btn-lime btn-lg"
              style={{ fontWeight: 800, letterSpacing: '-0.01em' }}
            >
              <Send size={18} />
              <span>Send Money</span>
            </Link>

            {activeWallet && (
              <button
                onClick={() => setDepositTargetWallet(activeWallet)}
                className="btn btn-secondary btn-lg"
                title={`Deposit demo funds into active ${activeWallet.currency} wallet`}
              >
                <Coins size={17} color="var(--lime)" />
                <span>Add Demo Funds</span>
              </button>
            )}

            <Link to="/profile" className="btn btn-secondary">
              <PlusCircle size={16} />
              <span>Open Currency Account</span>
            </Link>

            <Link to="/transactions" className="btn btn-ghost">
              <History size={16} />
              <span>View Ledger</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Wallets Overview */}
      <div style={{ marginBottom: '2.5rem' }}>
        <div className="surface-header">
          <div>
            <h2 className="surface-title">Wallet Nodes</h2>
            <div className="surface-subtitle">
              Pessimistic row-locked ledger balances by currency
            </div>
          </div>
          <Link to="/profile" className="btn btn-ghost btn-sm">
            Manage All
          </Link>
        </div>

        {isLoading ? (
          <div
            style={{
              padding: '3rem',
              textAlign: 'center',
              color: 'var(--muted)',
              background: 'var(--slate)',
              borderRadius: 'var(--radius-md)',
            }}
          >
            <RefreshCw size={24} className="spin" style={{ margin: '0 auto 0.75rem' }} />
            <div>Syncing wallet records...</div>
          </div>
        ) : wallets.length === 0 ? (
          <div
            className="surface"
            style={{
              textAlign: 'center',
              padding: '3.5rem 2rem',
              border: '1px dashed var(--slate-border-strong)',
            }}
          >
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--ivory)', marginBottom: '0.5rem' }}>
              No Active Ledger Accounts
            </h3>
            <p style={{ color: 'var(--muted)', maxWidth: '420px', margin: '0 auto 1.5rem', fontSize: '0.88rem' }}>
              Your node currently has zero currency wallets. Open a primary USD wallet to begin peer-to-peer liquidity transfers.
            </p>
            <button
              onClick={() => handleCreatePrimaryWallet('USD')}
              className="btn btn-lime"
              disabled={isCreatingPrimary}
            >
              <Plus size={16} />
              <span>{isCreatingPrimary ? 'Opening...' : 'Open Primary USD Wallet'}</span>
            </button>
          </div>
        ) : (
          <div className="wallet-grid">
            {wallets.map((wallet) => (
              <WalletCard
                key={wallet.id}
                wallet={wallet}
                onDeposit={(w) => setDepositTargetWallet(w)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Transaction Feed with Unmistakable Directional Indicators */}
      <div className="surface" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: '1.5rem 1.5rem 1rem' }}>
          <div className="surface-header" style={{ marginBottom: 0 }}>
            <div>
              <h2 className="surface-title">Recent Ledger Entries</h2>
              <div className="surface-subtitle">
                Guaranteed double-entry debits and credits
              </div>
            </div>
            <Link to="/transactions" className="btn btn-ghost btn-sm">
              Full Ledger ({recentTransfers.length})
            </Link>
          </div>
        </div>

        {recentTransfers.length === 0 ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--muted)' }}>
            <History size={32} style={{ margin: '0 auto 0.5rem', opacity: 0.4 }} />
            <p style={{ fontSize: '0.88rem' }}>No ledger transfers recorded yet.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="editorial-table">
              <thead>
                <tr>
                  <th>Flow</th>
                  <th>Amount</th>
                  <th>Counterparty</th>
                  <th>Settlement</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {recentTransfers.map((tx) => {
                  const isSender = wallets.some((w) => w.id === tx.senderWalletId)
                  return (
                    <tr key={tx.id}>
                      <td>
                        {isSender ? (
                          <span className="flow-badge flow-outgoing">
                            <ArrowUpRight size={13} />
                            <span>Debit / Sent</span>
                          </span>
                        ) : (
                          <span className="flow-badge flow-incoming">
                            <ArrowDownLeft size={13} />
                            <span>Credit / Recv</span>
                          </span>
                        )}
                      </td>
                      <td>
                        <span className={isSender ? 'amount-outgoing' : 'amount-incoming'}>
                          {isSender ? '-' : '+'}
                          {formatCurrency(tx.amount, tx.currency)}
                        </span>
                      </td>
                      <td>
                        <span className="font-mono" style={{ fontSize: '0.82rem', color: 'var(--ivory)' }}>
                          {isSender ? `To Wallet #${tx.recipientWalletId}` : `From Wallet #${tx.senderWalletId}`}
                        </span>
                      </td>
                      <td>
                        <span className="status-pill settled">
                          <ShieldCheck size={11} style={{ verticalAlign: 'middle', marginRight: '3px' }} />
                          {tx.status}
                        </span>
                      </td>
                      <td style={{ color: 'var(--muted)', fontSize: '0.8rem', fontFamily: 'var(--font-mono)' }}>
                        {formatDateTime(tx.createdAt)}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Demo Deposit Modal */}
      {depositTargetWallet && (
        <DemoDepositModal
          wallet={depositTargetWallet}
          onClose={() => setDepositTargetWallet(null)}
          onSuccess={async () => {
            await loadDashboardData()
          }}
        />
      )}
    </div>
  )
}
