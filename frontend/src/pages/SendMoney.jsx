import { useState, useEffect } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { walletService } from '../services/walletService'
import { transferService } from '../services/transferService'
import { generateUUID } from '../utils/uuid'
import { formatCurrency, formatDateTime } from '../utils/formatters'
import { ConfirmModal } from '../components/ConfirmModal'
import {
  Send,
  ArrowRight,
  AlertCircle,
  CheckCircle2,
  RefreshCw,
  Wallet,
  ShieldCheck,
  KeyRound,
  FileText,
  RotateCcw,
  Zap,
} from 'lucide-react'

export function SendMoney() {
  const [searchParams] = useSearchParams()

  const [wallets, setWallets] = useState([])
  const [selectedWalletId, setSelectedWalletId] = useState('')
  const [recipientWalletId, setRecipientWalletId] = useState('')
  const [amount, setAmount] = useState('')
  const [idempotencyKey, setIdempotencyKey] = useState(() => generateUUID())

  const [isLoadingWallets, setIsLoadingWallets] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isConfirmOpen, setIsConfirmOpen] = useState(false)
  const [error, setError] = useState(null)
  const [completedTransfer, setCompletedTransfer] = useState(null)

  useEffect(() => {
    async function fetchWallets() {
      setIsLoadingWallets(true)
      try {
        const data = await walletService.getWallets()
        setWallets(data || [])

        const paramId = searchParams.get('senderId')
        if (paramId && data.some((w) => String(w.id) === paramId)) {
          setSelectedWalletId(paramId)
        } else if (data.length > 0) {
          setSelectedWalletId(String(data[0].id))
        }
      } catch (err) {
        setError(err.message || 'Failed to query source wallets')
      } finally {
        setIsLoadingWallets(false)
      }
    }
    fetchWallets()
  }, [searchParams])

  const selectedWallet = wallets.find((w) => String(w.id) === String(selectedWalletId))
  const currency = selectedWallet?.currency || 'USD'
  const availableBalance = selectedWallet ? parseFloat(selectedWallet.balance) : 0

  function handleQuickAmount(value) {
    if (value === 'MAX') {
      setAmount(String(availableBalance))
    } else {
      setAmount(String(value))
    }
  }

  function handleInitiateTransfer(e) {
    e.preventDefault()
    setError(null)

    if (!selectedWalletId) {
      setError('Please select an active source wallet')
      return
    }

    const rId = parseInt(recipientWalletId, 10)
    if (isNaN(rId) || rId <= 0) {
      setError('Enter a valid numeric recipient wallet ID')
      return
    }

    if (String(rId) === String(selectedWalletId)) {
      setError('Self-transfer is prohibited: Source and recipient wallets must be distinct.')
      return
    }

    const numAmount = parseFloat(amount)
    if (isNaN(numAmount) || numAmount <= 0) {
      setError('Transfer amount must be strictly greater than 0.0000')
      return
    }

    const decimals = amount.includes('.') ? amount.split('.')[1].length : 0
    if (decimals > 4) {
      setError('Transfer amount cannot exceed 4 decimal places of precision')
      return
    }

    if (numAmount > availableBalance) {
      setError(
        `Insufficient settled liquidity: Requested ${formatCurrency(
          numAmount,
          currency
        )} exceeds available balance of ${formatCurrency(availableBalance, currency)}`
      )
      return
    }

    setIsConfirmOpen(true)
  }

  async function handleConfirmTransfer() {
    setIsSubmitting(true)
    setError(null)

    try {
      const response = await transferService.createTransfer({
        senderWalletId: selectedWalletId,
        recipientWalletId: recipientWalletId.trim(),
        amount: parseFloat(amount),
        currency: currency,
        idempotencyKey: idempotencyKey,
      })

      setCompletedTransfer(response)
      setIsConfirmOpen(false)
      walletService.getWallets().then((updated) => setWallets(updated || []))
    } catch (err) {
      setIsConfirmOpen(false)
      setError(err.message || 'Settlement failed. Please verify ledger constraints and try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  function handleResetForm() {
    setCompletedTransfer(null)
    setAmount('')
    setRecipientWalletId('')
    setIdempotencyKey(generateUUID())
    setError(null)
  }

  // Explicit Verified Settlement Receipt
  if (completedTransfer) {
    return (
      <div style={{ maxWidth: '580px', margin: '1rem auto' }}>
        <div
          className="surface"
          style={{
            textAlign: 'center',
            padding: '3rem 2.25rem',
            border: '1px solid var(--lime-border)',
            boxShadow: 'var(--shadow-hover)',
          }}
        >
          <div
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              background: 'var(--lime-dim)',
              border: '1px solid var(--lime-border)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '1.25rem',
            }}
          >
            <CheckCircle2 size={36} color="var(--lime)" />
          </div>

          <div
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: '0.72rem',
              color: 'var(--lime)',
              letterSpacing: '0.1em',
              textTransform: 'uppercase',
              marginBottom: '0.35rem',
            }}
          >
            DOUBLE-ENTRY COMMIT CONFIRMED
          </div>

          <h2 style={{ fontSize: '1.85rem', fontWeight: 800, color: 'var(--ivory)' }}>
            Transfer Settled
          </h2>
          <p style={{ color: 'var(--muted)', fontSize: '0.88rem', marginTop: '0.25rem' }}>
            Funds moved atomically across locked ledger partitions
          </p>

          <div
            style={{
              fontFamily: 'var(--font-display)',
              fontSize: '2.75rem',
              fontWeight: 800,
              color: 'var(--lime)',
              margin: '1.75rem 0',
              letterSpacing: '-0.03em',
            }}
          >
            {formatCurrency(completedTransfer.amount, completedTransfer.currency)}
          </div>

          <div
            style={{
              background: 'var(--navy)',
              borderRadius: 'var(--radius-sm)',
              padding: '1.25rem',
              textAlign: 'left',
              border: '1px solid var(--slate-border)',
              marginBottom: '1.75rem',
              fontSize: '0.86rem',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0' }}>
              <span style={{ color: 'var(--muted)' }}>Ledger Transfer ID:</span>
              <span className="font-mono" style={{ fontWeight: 700, color: 'var(--ivory)' }}>
                #{completedTransfer.id}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0' }}>
              <span style={{ color: 'var(--muted)' }}>Source Account:</span>
              <span className="font-mono">Wallet #{completedTransfer.senderWalletId}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0' }}>
              <span style={{ color: 'var(--muted)' }}>Recipient Account:</span>
              <span className="font-mono" style={{ color: 'var(--lime)' }}>
                Wallet #{completedTransfer.recipientWalletId}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0' }}>
              <span style={{ color: 'var(--muted)' }}>Status:</span>
              <span className="status-pill settled">
                <ShieldCheck size={11} style={{ verticalAlign: 'middle', marginRight: '3px' }} />
                {completedTransfer.status}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0' }}>
              <span style={{ color: 'var(--muted)' }}>Committed Timestamp:</span>
              <span className="font-mono" style={{ fontSize: '0.8rem' }}>
                {formatDateTime(completedTransfer.createdAt)}
              </span>
            </div>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                padding: '0.4rem 0',
                borderTop: '1px solid var(--slate-border)',
                marginTop: '0.4rem',
              }}
            >
              <span style={{ color: 'var(--muted)' }}>Cryptographic Key:</span>
              <span
                className="font-mono"
                style={{ fontSize: '0.72rem', color: 'var(--muted-dim)' }}
              >
                {completedTransfer.idempotencyKey.slice(0, 20)}...
              </span>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <button onClick={handleResetForm} className="btn btn-lime">
              <RotateCcw size={15} />
              <span>Send Another</span>
            </button>
            <Link to="/transactions" className="btn btn-secondary">
              <FileText size={15} />
              <span>Inspect Ledger</span>
            </Link>
            <Link to="/" className="btn btn-ghost">
              Return Home
            </Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div style={{ maxWidth: '640px', margin: '0 auto' }}>
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
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
            marginBottom: '0.5rem',
          }}
        >
          <Zap size={13} />
          <span>REAL-TIME P2P TRANSIT</span>
        </div>
        <h1 style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--ivory)' }}>
          Send Liquidity
        </h1>
        <p style={{ color: 'var(--muted)', fontSize: '0.9rem', marginTop: '0.2rem' }}>
          Move funds instantly to any registered peer wallet via atomic double-entry settlement
        </p>
      </div>

      {error && (
        <div className="alert alert-danger">
          <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>{error}</div>
        </div>
      )}

      {isLoadingWallets ? (
        <div className="surface" style={{ padding: '3.5rem', textAlign: 'center', color: 'var(--muted)' }}>
          <RefreshCw size={24} className="spin" style={{ margin: '0 auto 0.75rem' }} />
          <div>Loading source ledger partitions...</div>
        </div>
      ) : wallets.length === 0 ? (
        <div className="surface" style={{ textAlign: 'center', padding: '3rem 2rem' }}>
          <Wallet size={36} color="var(--lime)" style={{ margin: '0 auto 1rem' }} />
          <h3 style={{ color: 'var(--ivory)' }}>No Active Wallets</h3>
          <p style={{ color: 'var(--muted)', margin: '0.5rem 0 1.5rem', fontSize: '0.88rem' }}>
            You need at least one currency account with settled funds to initiate peer transfers.
          </p>
          <Link to="/profile" className="btn btn-lime">
            Open Wallet Account
          </Link>
        </div>
      ) : (
        <div className="surface" style={{ padding: '2.25rem' }}>
          <form onSubmit={handleInitiateTransfer}>
            {/* Source Wallet Selector */}
            <div className="form-group">
              <label className="form-label" htmlFor="source-wallet">
                <span>Source Wallet</span>
                {selectedWallet && (
                  <span className="form-label-hint">
                    Settled:{' '}
                    <strong style={{ color: 'var(--lime)' }}>
                      {formatCurrency(selectedWallet.balance, currency)}
                    </strong>
                  </span>
                )}
              </label>
              <select
                id="source-wallet"
                className="form-input font-mono"
                value={selectedWalletId}
                onChange={(e) => setSelectedWalletId(e.target.value)}
                required
              >
                {wallets.map((w) => (
                  <option key={w.id} value={w.id}>
                    WALLET #{w.id} — {w.currency} (Balance: {formatCurrency(w.balance, w.currency)})
                  </option>
                ))}
              </select>
            </div>

            {/* Recipient Wallet Identifier */}
            <div className="form-group">
              <label className="form-label" htmlFor="recipient-wallet-id">
                <span>Recipient Wallet ID</span>
                <span className="form-label-hint">Numeric account identifier</span>
              </label>
              <input
                id="recipient-wallet-id"
                type="number"
                min="1"
                step="1"
                className="form-input font-mono"
                placeholder="e.g. 2"
                value={recipientWalletId}
                onChange={(e) => setRecipientWalletId(e.target.value)}
                required
              />
            </div>

            {/* Transfer Amount Input */}
            <div className="form-group">
              <label className="form-label" htmlFor="transfer-amount">
                <span>Transfer Amount ({currency})</span>
                <span className="form-label-hint">Max 4 decimal places</span>
              </label>
              <input
                id="transfer-amount"
                type="number"
                min="0.0001"
                max={availableBalance}
                step="0.0001"
                className="form-input font-mono"
                style={{ fontSize: '1.25rem', fontWeight: 700 }}
                placeholder="0.0000"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />

              {/* Quick Preset Pills */}
              <div style={{ display: 'flex', gap: '0.45rem', marginTop: '0.65rem', flexWrap: 'wrap' }}>
                {[10, 50, 100, 250].map((val) => (
                  <button
                    key={val}
                    type="button"
                    onClick={() => handleQuickAmount(val)}
                    className="btn btn-secondary btn-sm"
                    style={{ fontSize: '0.72rem', padding: '0.25rem 0.55rem', fontFamily: 'var(--font-mono)' }}
                  >
                    +{val} {currency}
                  </button>
                ))}
                <button
                  type="button"
                  onClick={() => handleQuickAmount('MAX')}
                  className="btn btn-secondary btn-sm"
                  style={{
                    fontSize: '0.72rem',
                    padding: '0.25rem 0.55rem',
                    fontFamily: 'var(--font-mono)',
                    color: 'var(--lime)',
                    borderColor: 'var(--lime-border)',
                  }}
                >
                  MAX ({formatCurrency(availableBalance, currency)})
                </button>
              </div>
            </div>

            {/* Audit / Idempotency Section */}
            <div
              style={{
                marginTop: '1.75rem',
                padding: '0.9rem 1.1rem',
                borderRadius: 'var(--radius-sm)',
                background: 'var(--navy)',
                border: '1px solid var(--slate-border)',
                fontSize: '0.75rem',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  color: 'var(--muted)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <KeyRound size={13} color="var(--lime)" />
                  <span style={{ fontFamily: 'var(--font-mono)' }}>Idempotency Hash (Replay Guard):</span>
                </div>
                <button
                  type="button"
                  onClick={() => setIdempotencyKey(generateUUID())}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: 'var(--lime)',
                    cursor: 'pointer',
                    fontSize: '0.72rem',
                    fontFamily: 'var(--font-mono)',
                  }}
                  title="Generate a new cryptographic UUID key"
                >
                  Regenerate
                </button>
              </div>
              <div
                className="font-mono"
                style={{
                  color: 'var(--muted-dim)',
                  marginTop: '0.3rem',
                  fontSize: '0.7rem',
                  wordBreak: 'break-all',
                }}
              >
                {idempotencyKey}
              </div>
            </div>

            {/* Bold Lime Action Button */}
            <button
              type="submit"
              className="btn btn-lime btn-full btn-lg"
              style={{ marginTop: '2rem', fontWeight: 800 }}
              disabled={isSubmitting || availableBalance <= 0}
            >
              <Send size={18} />
              <span>Review & Commit Transfer</span>
              <ArrowRight size={18} />
            </button>
          </form>
        </div>
      )}

      {/* Reassuring Settlement Review Modal */}
      <ConfirmModal
        isOpen={isConfirmOpen}
        onClose={() => setIsConfirmOpen(false)}
        onConfirm={handleConfirmTransfer}
        isSubmitting={isSubmitting}
        details={{
          senderWalletId: selectedWalletId,
          recipientWalletId: recipientWalletId,
          amount: amount,
          currency: currency,
        }}
      />
    </div>
  )
}
