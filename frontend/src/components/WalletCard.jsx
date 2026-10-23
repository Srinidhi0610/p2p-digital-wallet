import { useState } from 'react'
import { Link } from 'react-router-dom'
import { formatCurrency } from '../utils/formatters'
import { Send, Copy, Check } from 'lucide-react'

export function WalletCard({ wallet }) {
  const [copied, setCopied] = useState(false)

  if (!wallet) return null

  const currency = wallet.currency?.toUpperCase() || 'USD'

  function handleCopy(e) {
    e.stopPropagation()
    navigator.clipboard.writeText(String(wallet.id))
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="architectural-wallet-card">
      {/* Abstract Money-in-Motion vector flow motif */}
      <svg
        className="wallet-card-watermark"
        width="160"
        height="140"
        viewBox="0 0 160 140"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M10 130 C 50 10, 110 130, 150 10"
          stroke="currentColor"
          strokeWidth="3"
          strokeDasharray="6 6"
        />
        <circle cx="150" cy="10" r="6" fill="var(--lime)" />
      </svg>

      {/* Card Header: Currency Pill & System Spec */}
      <div className="wallet-card-header">
        <span className="currency-pill">{currency} / LEDGER</span>
        <span
          style={{
            fontSize: '0.68rem',
            fontFamily: 'var(--font-mono)',
            color: 'var(--muted)',
            letterSpacing: '0.08em',
          }}
        >
          ACTIVE ROW-LOCK
        </span>
      </div>

      {/* Card Body: Expressive Balance */}
      <div className="wallet-balance-display">
        <div
          style={{
            fontSize: '0.72rem',
            fontFamily: 'var(--font-mono)',
            textTransform: 'uppercase',
            color: 'var(--muted)',
            letterSpacing: '0.1em',
            marginBottom: '0.35rem',
          }}
        >
          Settled Balance
        </div>
        <div className="wallet-balance-num">
          {formatCurrency(wallet.balance, currency)}
        </div>
      </div>

      {/* Card Actions Footer */}
      <div className="wallet-card-actions">
        <div className="wallet-id-chip">
          <span>WALLET #{wallet.id}</span>
          <button
            onClick={handleCopy}
            className="btn btn-ghost btn-sm"
            style={{ padding: '0.2rem 0.35rem', color: copied ? 'var(--lime)' : 'var(--muted)' }}
            title="Copy wallet identifier for incoming transfers"
          >
            {copied ? <Check size={13} color="var(--lime)" /> : <Copy size={13} />}
          </button>
        </div>

        <Link
          to={`/send?senderId=${wallet.id}&currency=${currency}`}
          className="btn btn-lime btn-sm"
          style={{
            padding: '0.35rem 0.85rem',
            fontSize: '0.78rem',
          }}
        >
          <Send size={13} />
          <span>Send</span>
        </Link>
      </div>
    </div>
  )
}
