import { useState } from 'react'
import { walletService } from '../services/walletService'
import { formatCurrency } from '../utils/formatters'
import {
  Coins,
  X,
  AlertCircle,
  Zap,
  Info,
  CheckCircle2,
} from 'lucide-react'

const QUICK_AMOUNTS = ['50', '100', '250', '500', '1000']

export function DemoDepositModal({ wallet, onClose, onSuccess }) {
  const [amount, setAmount] = useState('100.00')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)
  const [successResult, setSuccessResult] = useState(null)

  if (!wallet) return null

  const currency = wallet.currency?.toUpperCase() || 'USD'

  async function handleDeposit(e) {
    e.preventDefault()
    setError(null)

    const numAmount = parseFloat(amount)
    if (isNaN(numAmount) || numAmount <= 0) {
      setError('Please enter a valid deposit amount greater than 0.')
      return
    }

    if (numAmount > 100000) {
      setError('Maximum demo deposit limit is 100,000.00 per request.')
      return
    }

    setIsLoading(true)
    try {
      // Optional client-side idempotency key for network resilience
      const idempotencyKey = `demo-dep-${wallet.id}-${Date.now()}`
      const updatedWallet = await walletService.depositDemoFunds(
        wallet.id,
        numAmount,
        idempotencyKey
      )
      setSuccessResult(updatedWallet)
      if (onSuccess) {
        onSuccess(updatedWallet)
      }
    } catch (err) {
      setError(err.message || 'Failed to deposit demo funds.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        {/* Modal Header */}
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: 'var(--radius-xs)',
                background: 'var(--lime-dim)',
                color: 'var(--lime)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Coins size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--ivory)', margin: 0 }}>
                Add Demo Funds
              </h3>
              <div style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--lime)' }}>
                WALLET #{wallet.id} ({currency})
              </div>
            </div>
          </div>

          <button
            onClick={onClose}
            className="btn btn-ghost btn-sm"
            style={{ padding: '0.25rem' }}
            disabled={isLoading}
          >
            <X size={18} />
          </button>
        </div>

        {error && (
          <div className="alert alert-danger" style={{ marginBottom: '1.25rem' }}>
            <AlertCircle size={18} style={{ flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        )}

        {successResult ? (
          <div>
            <div
              style={{
                background: 'var(--navy)',
                borderRadius: 'var(--radius-sm)',
                padding: '1.5rem',
                border: '1px solid var(--lime-dim)',
                textAlign: 'center',
                marginBottom: '1.5rem',
              }}
            >
              <CheckCircle2
                size={40}
                color="var(--lime)"
                style={{ margin: '0 auto 0.75rem' }}
              />
              <h4 style={{ color: 'var(--ivory)', fontSize: '1.1rem', fontWeight: 800, marginBottom: '0.25rem' }}>
                Demo Funds Credited!
              </h4>
              <p style={{ color: 'var(--muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
                The double-entry ledger transfer was committed to your wallet.
              </p>
              <div
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: '1.4rem',
                  fontWeight: 800,
                  color: 'var(--lime)',
                }}
              >
                New Balance: {formatCurrency(successResult.balance, successResult.currency)}
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button
                type="button"
                className="btn btn-lime btn-full"
                onClick={onClose}
              >
                Done
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleDeposit}>
            {/* Current Balance Spec */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '0.75rem 1rem',
                background: 'var(--navy)',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--slate-border)',
                marginBottom: '1.25rem',
              }}
            >
              <span style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>Current Settled Balance:</span>
              <span className="font-mono" style={{ fontWeight: 700, color: 'var(--ivory)' }}>
                {formatCurrency(wallet.balance, currency)}
              </span>
            </div>

            {/* Quick Amount Chips */}
            <div style={{ marginBottom: '1.25rem' }}>
              <label className="form-label" style={{ marginBottom: '0.45rem' }}>
                Quick Amounts ({currency})
              </label>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                {QUICK_AMOUNTS.map((quickVal) => (
                  <button
                    key={quickVal}
                    type="button"
                    onClick={() => setAmount(quickVal + '.00')}
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: '0.78rem',
                      fontWeight: 700,
                      padding: '0.35rem 0.75rem',
                      borderRadius: 'var(--radius-pill)',
                      background: amount === quickVal + '.00' ? 'var(--lime)' : 'var(--navy)',
                      color: amount === quickVal + '.00' ? 'var(--lime-text)' : 'var(--muted)',
                      border: '1px solid',
                      borderColor: amount === quickVal + '.00' ? 'var(--lime)' : 'var(--slate-border)',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    +{quickVal}
                  </button>
                ))}
              </div>
            </div>

            {/* Custom Amount Field */}
            <div className="form-group">
              <label className="form-label" htmlFor="deposit-amount">
                Deposit Amount ({currency})
              </label>
              <input
                id="deposit-amount"
                type="number"
                step="0.01"
                min="0.0001"
                max="100000.00"
                className="form-input font-mono"
                placeholder="100.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
                disabled={isLoading}
              />
            </div>

            {/* Dev Environment Disclaimer Banner */}
            <div
              style={{
                display: 'flex',
                gap: '0.65rem',
                alignItems: 'flex-start',
                padding: '0.75rem 0.95rem',
                background: 'rgba(19, 29, 45, 0.6)',
                border: '1px solid var(--slate-border)',
                borderRadius: 'var(--radius-sm)',
                marginBottom: '1.5rem',
                fontSize: '0.78rem',
                color: 'var(--muted)',
                lineHeight: 1.45,
              }}
            >
              <Info size={16} color="var(--lime)" style={{ flexShrink: 0, marginTop: '2px' }} />
              <div>
                <strong style={{ color: 'var(--ivory)' }}>Development Sandbox Faucet:</strong> Liquidity is disbursed directly from the system test treasury node to test peer transfers. No actual bank rail or card charges are processed.
              </div>
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={onClose}
                disabled={isLoading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-lime"
                disabled={isLoading}
              >
                <Zap size={15} />
                <span>{isLoading ? 'Disbursing...' : 'Disburse Demo Funds'}</span>
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
