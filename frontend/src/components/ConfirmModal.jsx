import { ArrowRight, ShieldCheck, X } from 'lucide-react'
import { formatCurrency } from '../utils/formatters'

export function ConfirmModal({ isOpen, onClose, onConfirm, details, isSubmitting }) {
  if (!isOpen || !details) return null

  const { senderWalletId, recipientWalletId, amount, currency } = details

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: 'var(--radius-xs)',
                background: 'var(--lime-dim)',
                border: '1px solid var(--lime-border)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <ShieldCheck size={18} color="var(--lime)" />
            </div>
            <div>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--ivory)' }}>
                Review & Settle
              </h3>
              <div style={{ fontSize: '0.72rem', fontFamily: 'var(--font-mono)', color: 'var(--muted)' }}>
                P2P DOUBLE-ENTRY COMMIT
              </div>
            </div>
          </div>
          <button
            onClick={onClose}
            className="btn btn-ghost btn-sm"
            style={{ padding: '0.25rem' }}
            disabled={isSubmitting}
          >
            <X size={18} />
          </button>
        </div>

        <div style={{ marginBottom: '1.75rem' }}>
          {/* Transfer Flow Box */}
          <div
            style={{
              background: 'var(--navy)',
              border: '1px solid var(--slate-border)',
              borderRadius: 'var(--radius-sm)',
              padding: '1.25rem',
              marginBottom: '1.25rem',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: '1rem',
              }}
            >
              <div>
                <span style={{ fontSize: '0.68rem', fontFamily: 'var(--font-mono)', color: 'var(--muted)', display: 'block' }}>
                  SOURCE WALLET
                </span>
                <span className="font-mono" style={{ fontWeight: 700, color: 'var(--ivory)' }}>
                  ID #{senderWalletId}
                </span>
              </div>
              <ArrowRight size={16} color="var(--lime)" />
              <div style={{ textAlign: 'right' }}>
                <span style={{ fontSize: '0.68rem', fontFamily: 'var(--font-mono)', color: 'var(--muted)', display: 'block' }}>
                  RECIPIENT WALLET
                </span>
                <span className="font-mono" style={{ fontWeight: 700, color: 'var(--lime)' }}>
                  ID #{recipientWalletId}
                </span>
              </div>
            </div>

            <div
              style={{
                borderTop: '1px solid var(--slate-border)',
                paddingTop: '0.85rem',
                textAlign: 'center',
              }}
            >
              <span style={{ fontSize: '0.68rem', fontFamily: 'var(--font-mono)', color: 'var(--muted)', display: 'block' }}>
                TOTAL SETTLEMENT AMOUNT
              </span>
              <span
                style={{
                  fontFamily: 'var(--font-display)',
                  fontSize: '2rem',
                  fontWeight: 800,
                  color: 'var(--ivory)',
                  letterSpacing: '-0.03em',
                }}
              >
                {formatCurrency(amount, currency)}
              </span>
            </div>
          </div>

          <div
            style={{
              padding: '0.85rem 1rem',
              borderRadius: 'var(--radius-sm)',
              background: 'rgba(242, 168, 138, 0.08)',
              border: '1px solid var(--coral-border)',
              fontSize: '0.78rem',
              color: 'var(--coral)',
              lineHeight: 1.5,
            }}
          >
            <strong>Immutable Ledger Notice:</strong> This operation applies pessimistic locks across both accounts and appends matching DEBIT and CREDIT records. Balance updates cannot be reversed.
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-lime"
            onClick={onConfirm}
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Settling Ledger...' : 'Commit & Transfer'}
          </button>
        </div>
      </div>
    </div>
  )
}
